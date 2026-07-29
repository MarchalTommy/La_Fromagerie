---
name: fromagerie-firestore-data-model
description: >
  The Firestore data contract for LaFromagerie. Load before reading/writing any Firestore
  collection, mapping a document to a model, changing a field, or reasoning about the order
  lifecycle or cache invalidation. Covers each collection (products, orders, delivery_paths,
  preparation_status, database_update) with REAL field names/types and owning classes, which
  flavor reads/writes each, the OrderStatus lifecycle as implemented (client writes
  PENDING/PAID/CANCELED; admin writes IN_PREPARATION/DELIVERED), the database_update
  cache-invalidation flow, the Room caches, and the additive-only schema-evolution discipline
  (old APKs are still in the field). Also the hard rule: never write to prod Firestore from a
  dev session.
---

# LaFromagerie — Firestore Data Model

The data contract between the app and its ONLY backend of record: production Cloud Firestore.
There is no staging database. **Every field name and type below was read from the mapping
code as of 2026-07-06** (HEAD `b97eb83`), with `delivery_paths` (§2.3) and the Room caches (§5)
re-verified 2026-07-29 against PR #59. This is a real shop's real order data.

> **HARD RULE — production Firestore is sacred.** Never write to or delete from any prod
> collection from a dev/AI session. Inspect **read-only** via the Firebase console. See §6.

## Jargon, defined once

| Term | Meaning here |
|---|---|
| **Collection** | A top-level Firestore collection (a table-like bucket of documents). |
| **Document** | One record; has an `id` (the Firestore document id) plus a field map. |
| **DTO / data model** | The Kotlin `@Serializable` class that (de)serializes a document, e.g. `OrderData`. |
| **Owning class** | The datasource that reads/writes that collection. |
| **`toObject`** | Firebase's reflective document→class mapper; used only where field names match exactly. |

## When NOT to use this skill

- Payment mechanics that produce the order write → **fromagerie-payments-reference**.
- Module/layer/DI structure, Room-as-cache invariant → **fromagerie-architecture-contract**.
- How a schema/data change gets gated & merged → **fromagerie-change-control**.
- Secret/config wiring (Firebase project, google-services.json) → **fromagerie-config-and-secrets**.
- Delivery-routing domain logic (not the stored path shape) → **fromagerie-delivery-logistics-reference**.

---

## 1. Collections at a glance

| Collection | Doc id | Written by | Read by | Owning class(es) |
|---|---|---|---|---|
| `products` | Firestore auto-id | **admin** flavor | client + admin | `FirestoreAdminDatasource` (write), `FirestoreDatabase` (client read) |
| `orders` | `order.id` (client-chosen) | **client** (create) + **admin** (status) | admin | `FirestoreOrderDataSource` (client), `FirestoreAdminDatasource.getAllOrders` (admin) |
| `delivery_paths` | Firestore auto-id | **admin** flavor | client + admin | `FirestoreAdminDatasource` (write), `FirestoreDeliveryDataSource` (read) |
| `preparation_status` | `status.id` | **admin** flavor | admin | `FirestoreAdminDatasource` |
| `database_update` | fixed ids `products_timestamp`, `path_timestamp` | **admin** flavor | client (+ admin) | `FirestoreAdminDatasource` (write), `FirestoreDatabase.getLastDatabaseUpdate` (read) |

Regenerate this map any time:
```bash
grep -rn 'collection(' . | grep '\.kt:' | grep -v /build/
```

---

## 2. Field contracts (real names and types)

### 2.1 `products`  — DTO `ProductData` (`core/data/.../model/ProductData.kt`)

Firestore stores product documents with these fields (the client read path is
`home/data/.../FirestoreDatabase.getAllProducts`; the admin write path is
`FirestoreAdminDatasource`):

| Field | Type | Notes |
|---|---|---|
| `name` | String | default `"Unknown Cheese"` |
| `priceCents` | Long (**cents**) | money-as-cents invariant (see architecture-contract I1) |
| `imgUrl` | String? | Cloudinary URL |
| `type` | String | maps to `ProductType` enum via `toProductType()`; `getAllCheeses()` queries `whereEqualTo("type", "cheese")` |
| `description` | String | `\\n` stored, converted to real newlines on read |
| `allergens` | List&lt;String&gt;? | |
| availability | Boolean | ⚠️ **naming is inconsistent** — see below |

⚠️ **`isAvailable` vs `available` mismatch (real, verify before trusting):** `ProductData`
declares `@SerializedName("isAvailable")`, but the client manual reader
(`FirestoreDatabase.getAllProducts`) reads `item.data?.get("available")` for the full-schema
branch and `"h"` for the legacy branch. Do not assume one canonical key; check both when
touching availability.

⚠️ **Legacy short-key schema still supported:** `getAllProducts` has a fallback branch —
if a document has no `name` field, it reads short keys `b`=name, `c`=priceCents(Long),
`d`=imgUrl, `e`=type, `f`=description, `g`=allergens, `h`=available. This is old data still
in prod. **Do not delete this branch** without confirming no such documents remain.

### 2.2 `orders`  — DTO `OrderData` (`core/data/src/main/java/com/mtdevelopment/core/model/OrderData.kt`, `@Serializable`, snake_case)

| Field | Type | Notes |
|---|---|---|
| `id` | String | also the Firestore document id (`.document(orderData.id).set(...)`) |
| `customer_name` | String | |
| `customer_address` | String | |
| `billing_address` | String | |
| `delivery_date` | String | `"dd/MM/yyyy"` (date invariant) |
| `order_date` | String | `"dd/MM/yyyy"` |
| `products` | Map&lt;String, Int&gt; | product name → quantity. Firestore stores numbers as Long; the admin reader defensively casts via `(value as? Number)?.toInt()` |
| `status` | String (enum name) | `OrderStatus` name; written as `newStatus.name`, read with `runCatching { valueOf(...) }.getOrDefault(PENDING)` |
| `note` | String? | |
| `is_manually_added` | Boolean? | admin-created manual orders |

**Mapping asymmetry to know:** the admin reader (`getAllOrders`) maps documents **manually**
(snake_case + explicit `OrderStatus` conversion, skipping malformed docs) because
`toObject` cannot convert the string status. The client writer uses `.set(orderData)` via
the `@Serializable` DTO. Unknown/newer status values from a future app version degrade to
`PENDING` rather than crashing — a forward-compat safety net; preserve it.

### 2.3 `delivery_paths`  — `city_entries` is canonical (reshaped 2026-07-29, PR #59)

A path covers a set of cities, and **each city may carry its own street allow-list** so two
paths can split one city between them by street. That per-(path, city) scoping is the whole
point of the shape — see `fromagerie-delivery-logistics-reference` for the matching rules it
feeds.

| Field | Type | Notes |
|---|---|---|
| `path_name` | String | |
| `delivery_day` | String | a `DayOfWeek` enum name |
| `delivery_frequency` | String | `WEEKLY` / `BIWEEKLY_EVEN` / `BIWEEKLY_ODD` |
| `city_entries` | List&lt;Map&gt; | **canonical.** Each entry: `city` (String), `postcode` (Int), `streets` (List&lt;String&gt;). Empty/absent `streets` = the path serves the whole city. |
| `cities` | List&lt;String&gt; | legacy parallel array, still written — see below |
| `postcodes` | List&lt;Int&gt; | legacy parallel array, still written — see below |

- **Write DTO (admin):** `DataDeliveryPath` + `DataDeliveryCity`
  (`admin/data/.../model/DataDeliveryPath.kt`). ⚠️ Firestore's `set`/`add` use their own
  reflective POJO mapper, **not** kotlinx.serialization, so on this class the **Kotlin property
  names are the Firestore field names** — that is why they are snake_case. A `@SerialName` here
  would be decorative and would not rename anything.
- **Read DTO (client+admin):** `DataDeliveryPathsResponse` + `DataDeliveryCityResponse`
  (`delivery/data/.../model/response/firestore/`), populated by **manual map reads** in
  `FirestoreDeliveryDataSource` — so again the raw document keys are the contract, not the
  `@SerialName` annotations. `toDeliveryCities()` resolves the document to domain
  `DeliveryCity` objects.

**Both shapes are read, one is canonical.** `toDeliveryCities()` prefers `city_entries` and
falls back to zipping `cities`/`postcodes` when it is absent or empty. Documents written before
PR #59 therefore keep working untouched and convert to the new shape the first time the admin
saves that path — **there was no data migration and no prod write**.

`cities`/`postcodes` are still written, but as a **one-way projection** of `city_entries`
computed in `toDataDeliveryPath()`. They exist for older clients only; new code must never read
them back. Because the projection lives in that single mapper, the two shapes cannot drift —
if you ever find yourself setting them independently, that is the bug.

⚠️ **The path-level `streets` field is deliberately never written again.** It used to mean "this
whole path serves only these streets", which a client applies across *every* city of the path —
so a path restricting Boujailles would make Frasne and Courvière undeliverable too. Its absence
is the chosen degradation: a client that only understands the old field sees every city as fully
covered, so a customer may land on the wrong tournée but is **never blocked**. Do not
"helpfully" restore it as a union of the per-city lists — that reintroduces the exact bug.

⚠️ **`cities` and `postcodes` are zipped positionally** on the fallback path, so the two arrays
must stay the same length and order. This is the hazard `city_entries` exists to retire; prefer
it for anything new.

⚠️ **Numbers come back as `Long`.** Firestore has no Int. Read postcodes via
`(value as? Number)?.toInt()`, never `as? List<Int>` — the latter is erased at runtime, so the
bad cast succeeds here and blows up later at the point of use. (Fixed in PR #59; the same
defensive pattern is already used by the `orders` reader, §2.2.)

⚠️ **Historical, resolved 2026-07-29:** the reader used to expect a path-level `streets` field
that the write DTO never wrote, so anything the admin typed into the path dialog was silently
dropped on save. Both halves are now covered by tests (`DataDeliveryPathTest` in `admin/data`,
`DataDeliveryPathsResponseTest` in `delivery/data`).
2. `FirestoreDeliveryDataSource.getDeliveryPath` **used to** query
   `whereEqualTo("pathName", pathName)` while the stored field is `path_name`, so it filtered
   on a non-existent field and never matched. **FIXED 2026-07-07 (commit `58f85c9`, PR #43):**
   the query now uses `path_name` (matching the write DTO and `getAllDeliveryPaths`), and
   `FirestoreDeliveryDataSourceTest` asserts the field name as a regression guard. No stored
   field changed — read-query fix only, so no schema/compat impact. (The method is still
   uninvoked in main source; the fix makes it correct for when it is wired up.)

### 2.4 `preparation_status`  — DTO `PreparationStatusData` (`core/data/.../model/PreparationStatusData.kt`)

| Field | Type | Notes |
|---|---|---|
| `id` | String | document id |
| `date` | String | |
| `product_name` | String | `@SerialName("product_name")`, Kotlin `productName` |
| `is_prepared` | Boolean | `@SerialName("is_prepared")` |

Admin-only. Written/read via `FirestoreAdminDatasource.updatePreparationStatus` /
`getPreparationStatuses` (uses `toObject`, so names must match exactly).

### 2.5 `database_update`  — cache-invalidation timestamps

Exactly two fixed documents:

| Doc id | Field | Type |
|---|---|---|
| `products_timestamp` | `last_update` | Firestore `Timestamp` |
| `path_timestamp` | `last_update` | Firestore `Timestamp` |

Written by admin (`saveNewDatabaseProductUpdate` / `saveNewDatabasePathsUpdate`,
`set(mapOf("last_update" to Timestamp(...)))`). Read by client
(`FirestoreDatabase.getLastDatabaseUpdate`) into `FirestoreUpdateData`
(`products_timestamp`/`path_timestamp` → epoch millis).

---

## 3. Order lifecycle (OrderStatus, as implemented)

Enum `OrderStatus` (`core/domain/.../model/OrderStatus.kt`):
`PENDING, PAID, IN_PREPARATION, PREPARED, IN_DELIVERY, DELIVERED, CANCELED`.

**Transitions that actually happen in code (verified by grep of writers):**

```
   client checkout                              admin
   ────────────────                             ─────
   create order  → PENDING
   payment succeeds (WorkManager) → PAID
   payment fails (WorkManager)    → CANCELED
                                                 order preparation → IN_PREPARATION
                                                 delivery done     → DELIVERED
```

- **Client writes:** `PENDING` on order creation
  (`CheckoutViewModel` ~L457), `PAID` / `CANCELED` from the durable finalizer
  (`FinalizePaymentWorker`: `CHECKOUT_STATUS.PAID → OrderStatus.PAID`,
  `FAILED → OrderStatus.CANCELED`) and `CheckoutViewModel` ~L412. Status writes go through
  `FirestoreOrderDataSource.updateOrder(orderId, newStatus)` → `.update("status", name)`.
- **Admin writes:** `IN_PREPARATION` and `DELIVERED` (in `DeliveryHelperScreen`,
  `DeliveryAddDialog`). Admin filters out `CANCELED` orders from its list
  (`GetAllOrdersUseCase`).

⚠️ **`PREPARED` and `IN_DELIVERY` enum values are defined but never written anywhere** (grep
of non-test main source finds no setters as of 2026-07-06). Treat them as reserved/vestigial;
don't assume an order ever holds them. Re-verify:
`grep -rn 'OrderStatus\.\(PREPARED\|IN_DELIVERY\)' . | grep '\.kt' | grep -v /build/ | grep -v enum`.

---

## 4. `database_update` cache-invalidation, end to end

The convention that keeps client caches fresh without polling every product:

1. **Admin writes data** (add/update/delete product or path) and then **bumps the matching
   timestamp** by calling `saveNewDatabaseProductUpdate(now)` / `saveNewDatabasePathsUpdate(now)`
   → sets `database_update/{products_timestamp|path_timestamp}.last_update`.
2. **Client, on load,** runs `GetLastFirestoreDatabaseUpdateUseCase`
   (`home/domain/.../GetLastFirestoreDatabaseUpdateUseCase.kt`):
   - fetches remote timestamps,
   - compares against locally stored ones in `SharedDatastore`
     (`lastFirestoreProductsUpdate`, `lastFirestorePathsUpdate`),
   - if `remote != local` **or** local is `0L`, sets `shouldRefreshProducts` /
     `shouldRefreshPaths` flags,
   - updates the stored local timestamps to match remote.
3. Those refresh flags drive the client to re-fetch from Firestore and repopulate Room.

**Staleness behavior / the trap:** the client only knows to refresh **if the admin bumped
the timestamp**. If you write to `products` or `delivery_paths` **without** bumping the
corresponding `database_update` doc, clients will keep serving stale Room cache until the
timestamp changes or the cache is destroyed. **Any code path that writes products/paths MUST
also bump the timestamp.** (This is why the admin datasource pairs writes with
`saveNewDatabase*Update`.)

---

## 5. Room caches (the local mirror)

There is **one** Room `@Database`: `FromagerieDatabase` (in `app`, schema **version 6** as of
2026-07-29), with entities:

| Entity | Table | Columns |
|---|---|---|
| `ProductEntity` (`home/data`) | `products` | `id`, `name`, `priceInCents` (Long), `imageUrl`, `type`, `description`, `allergens`, `isAvailable` |
| `PathEntity` (`delivery/data`) | `paths` | `id`, `name`, `availableCities` (Map&lt;String,Int&gt;), `cityStreets` (Map&lt;String,List&lt;String&gt;&gt;), `locations` (List&lt;Coordinate&gt;), `deliveryDay`, `deliveryFrequency`, `geojson` (String) |

⚠️ **Room column names are the Kotlin property names, NOT the `@SerialName` values.**
`PathEntity` carries both annotations — `@SerialName("cities")` sits on a property called
`availableCities` — but nothing sets `@ColumnInfo`, so the column is `availableCities`. The
`@SerialName`s serve kotlinx serialization only. Confirmed by the migrations themselves, which
alter `deliveryFrequency` and `cityStreets`, i.e. the property names. Get this backwards and you
write a migration against a column that does not exist.

⚠️ **`HomeDatabase` and `DeliveryDatabase` are NOT Room databases.** Despite the name, they
are thin **facade wrappers around a DAO** (`HomeDao`, `DeliveryDao`) — plain classes with
persist/update/delete/get methods, injected as Koin `factory { HomeDatabase(get()) }`. The
actual DB and DAOs come from `FromagerieDatabase` (`db.homeDao`, `db.deliveryDao`, see
`AppModule.kt`).

### Migrations — real ones, with the destructive fallback still armed behind them

`AppModule.provideDataBase` registers `.addMigrations(MIGRATION_4_5, MIGRATION_5_6)` **and**
`.fallbackToDestructiveMigration(true)`. Both live in `FromagerieDatabase.kt`:

| Migration | Does |
|---|---|
| `MIGRATION_4_5` | `ALTER TABLE paths ADD COLUMN deliveryFrequency TEXT NOT NULL DEFAULT 'WEEKLY'` |
| `MIGRATION_5_6` | `ALTER TABLE paths ADD COLUMN cityStreets TEXT NOT NULL DEFAULT '{}'` (2026-07-29, PR #59) |

The fallback is a **safety net for unhandled version jumps, not the intended path**. Prefer
writing a real additive migration: the cache being wiped is not harmless in practice — a first
launch with no network and an empty `paths` table makes **every address undeliverable**, which
is a bug that actually shipped (fixed 2026-07-29, see the note at the end of §6). Each Map-typed
column needs its own `@TypeConverter`; `cityStreets` is served by `StreetsMapConverter`,
distinct from `MapConverter` which handles `Map<String,Int>`.

**Refresh triggers:** Room is repopulated when the `database_update` comparison (§4) sets a
refresh flag. Bumping the schema version without a migration wipes the local cache and forces a
re-fetch from Firestore — never a reason to skip the Firestore-timestamp bump.

---

## 6. Schema-evolution discipline (old APKs are in the field)

Client and admin APKs from earlier versions are still installed on real devices and are
still reading and writing prod. Therefore:

- **Additive-only.** Add new fields with safe defaults. Do NOT rename, remove, or repurpose
  an existing field — an old APK will read the missing field as null/default and may write
  the old shape back. (Evidence this is real: the legacy `b..h` product keys still coexist with
  the current product shape, §2.1.)
- **When a shape genuinely has to change, migrate by reading both and writing one.** Teach the
  reader to accept the old and new shapes, make the writer emit the new one, and let documents
  convert as they are re-saved. That is how `delivery_paths` moved to `city_entries` (§2.3) with
  no data migration, no flag day, and no prod write. Reach for this before considering a
  breaking change.

  ⚠️ **Check whether the app has actually shipped before invoking this section's premise.** On
  2026-07-29 Tommy stated the app was **not yet in production** (launch expected within days),
  which is why PR #59 was allowed to reshape `delivery_paths` rather than bolt a field onto it.
  That window has almost certainly closed — **ask, do not infer**, and treat additive-only as
  binding unless he says otherwise.
- **Never change a field's type or semantics in place.** `priceCents` staying Long-cents is
  load-bearing (money invariant).
- **New enum values must degrade gracefully.** Readers already default unknown `OrderStatus`
  to `PENDING`; keep that pattern for any new status.
- **Bump the `database_update` timestamp** whenever you change product/path data (§4).

**Safe prod inspection (read-only only):**
- Use the **Firebase console** to view collections/documents. Read, never write.
- Do NOT run app write paths, scripts, or the admin flavor against prod to "test" a schema
  change from a dev session.
- Schema changes are a **class (e) change** in **fromagerie-change-control** — surface to
  Tommy before merge.

**Explicit prohibition:** dev/AI sessions must not write to prod collections. If you need to
prove a mapping works, write a **unit test** against the DTO (the DTOs are `@Serializable`
data classes; test the `toX()/fromX()` mappers), not a live Firestore write.

---

## Provenance and maintenance

| Claim | Re-verification command |
|---|---|
| Collection names & owners | `grep -rn 'collection(' . \| grep '\.kt:' \| grep -v /build/` |
| `OrderData` fields | `sed -n '1,30p' core/data/src/main/java/com/mtdevelopment/core/model/OrderData.kt` |
| `ProductData` fields + isAvailable/available split | read `core/data/.../model/ProductData.kt` and `home/data/.../FirestoreDatabase.kt` |
| legacy `b..h` product keys | `grep -n '"b"\|"h"' home/data/src/main/java/com/mtdevelopment/home/data/source/remote/FirestoreDatabase.kt` |
| delivery_paths document keys actually read | `grep -nE '"[a-z_]+"\]\|get\("' delivery/data/src/main/java/com/mtdevelopment/delivery/data/source/remote/FirestoreDeliveryDataSource.kt` (expect `city_entries` + legacy `cities`/`postcodes`; `streets` should appear **only** nested inside a `city_entries` element, never as a top-level `get("streets")`) |
| delivery_paths keys actually written | `grep -n 'val ' admin/data/src/main/java/com/mtdevelopment/admin/data/model/DataDeliveryPath.kt` (property names ARE the field names; a `streets` property reappearing is a regression) |
| `city_entries` ↔ legacy precedence still holds | `./gradlew :delivery:data:testClientDebugUnitTest :admin:data:testClientDebugUnitTest` (`DataDeliveryPathsResponseTest`, `DataDeliveryPathTest`) |
| OrderStatus values | `cat core/domain/src/main/java/com/mtdevelopment/core/model/OrderStatus.kt` |
| status writers | `grep -rn 'OrderStatus\.' . \| grep '\.kt:' \| grep -v /build/ \| grep -v 'src/test\|enum class'` |
| PREPARED/IN_DELIVERY unused | `grep -rn 'OrderStatus\.\(PREPARED\|IN_DELIVERY\)' . \| grep '\.kt' \| grep -v /build/ \| grep -v enum` (no writers = still vestigial) |
| cache-invalidation logic | read `home/domain/.../GetLastFirestoreDatabaseUpdateUseCase.kt` |
| Room `@Database` version/entities/migrations | `grep -n '@Database\|version =\|^val MIGRATION\|class .*Converter' app/src/main/java/com/mtdevelopment/lafromagerie/FromagerieDatabase.kt; grep -n 'addMigrations\|fallbackToDestructive' app/src/main/java/com/mtdevelopment/lafromagerie/di/AppModule.kt` (expect version 6 + `MIGRATION_4_5`, `MIGRATION_5_6`) |
| Room columns = Kotlin property names | `grep -n 'val [a-zA-Z]*:' delivery/data/src/main/java/com/mtdevelopment/delivery/data/model/entity/PathEntity.kt` and check the `ALTER TABLE` statements name those, not the `@SerialName`s |
| Home/Delivery DB are facades | `head -10 home/data/.../source/local/HomeDatabase.kt` (no `@Database` = still a wrapper) |

If any output diverges from this skill, update and date-stamp it.
