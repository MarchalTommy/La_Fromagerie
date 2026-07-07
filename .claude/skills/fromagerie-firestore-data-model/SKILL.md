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
code as of 2026-07-06** (HEAD `b97eb83`). This is a real shop's real order data.

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

### 2.3 `delivery_paths`  — two DTOs, note the divergence

- **Write DTO (admin):** `DataDeliveryPath` (`admin/data/.../model/DataDeliveryPath.kt`):
  `id`, `path_name` (String?), `delivery_day` (String), `cities` (List&lt;String&gt;),
  `postcodes` (List&lt;Int&gt;). Cities+postcodes are stored as two parallel lists.
- **Read DTO (client+admin):** `DataDeliveryPathsResponse`
  (`delivery/data/.../FirestoreDeliveryDataSource`) reads `path_name`, `cities`,
  `delivery_day`, `postcodes`, **and `streets` (List&lt;String&gt;)**.

⚠️ **Two real inconsistencies in `delivery_paths` — verify before editing:**
1. The reader expects a **`streets`** field that the admin write DTO (`DataDeliveryPath`)
   does **not** write. Streets are populated by other write paths (fine-grained streets, see
   git `bb3d36a`), so `streets` may be absent on older path docs — the reader defaults to
   `emptyList()`. Additive field; keep defaulting.
2. `FirestoreDeliveryDataSource.getDeliveryPath` queries
   `whereEqualTo("pathName", pathName)` — but the stored field is `path_name`. This query
   filters on a field that does not exist and will not match. Looks like a latent bug; do
   not "rely" on this method returning results. (Report via change-control, don't silently
   rename in prod.)

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

There is **one** Room `@Database`: `FromagerieDatabase` (in `app`, schema **version 4**,
`fallbackToDestructiveMigration(true)`), with entities:

| Entity | Table | Key fields |
|---|---|---|
| `ProductEntity` (`home/data`) | `products` | `id`, `name`, `priceInCents` (Long), `imageUrl`, `type`, `description`, `allergens`, `isAvailable` |
| `PathEntity` (`delivery/data`) | `paths` | `id`, `name`, `availableCities` (Map&lt;String,Int&gt;), `locations` (List&lt;Coordinate&gt;), `deliveryDay`, `geojson` (String) |

⚠️ **`HomeDatabase` and `DeliveryDatabase` are NOT Room databases.** Despite the name, they
are thin **facade wrappers around a DAO** (`HomeDao`, `DeliveryDao`) — plain classes with
persist/update/delete/get methods, injected as Koin `factory { HomeDatabase(get()) }`. The
actual DB and DAOs come from `FromagerieDatabase` (`db.homeDao`, `db.deliveryDao`, see
`AppModule.kt`).

**Refresh triggers:** Room is repopulated when the `database_update` comparison (§4) sets a
refresh flag. Because migration is destructive, bumping the Room schema version wipes the
local cache and it re-fetches from Firestore — acceptable, but never a reason to skip the
Firestore-timestamp bump.

---

## 6. Schema-evolution discipline (old APKs are in the field)

Client and admin APKs from earlier versions are still installed on real devices and are
still reading and writing prod. Therefore:

- **Additive-only.** Add new fields with safe defaults. Do NOT rename, remove, or repurpose
  an existing field — an old APK will read the missing field as null/default and may write
  the old shape back. (Evidence this is real: the legacy `b..h` product keys and the
  optional `streets` path field both coexist with newer shapes.)
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
| delivery_paths `streets`/`pathName` quirks | read `delivery/data/.../FirestoreDeliveryDataSource.kt` and `admin/data/.../model/DataDeliveryPath.kt` |
| OrderStatus values | `cat core/domain/src/main/java/com/mtdevelopment/core/model/OrderStatus.kt` |
| status writers | `grep -rn 'OrderStatus\.' . \| grep '\.kt:' \| grep -v /build/ \| grep -v 'src/test\|enum class'` |
| PREPARED/IN_DELIVERY unused | `grep -rn 'OrderStatus\.\(PREPARED\|IN_DELIVERY\)' . \| grep '\.kt' \| grep -v /build/ \| grep -v enum` (no writers = still vestigial) |
| cache-invalidation logic | read `home/domain/.../GetLastFirestoreDatabaseUpdateUseCase.kt` |
| Room `@Database` version/entities | `grep -rn '@Database\|fallbackToDestructiveMigration' app/src/main --include='*.kt'` |
| Home/Delivery DB are facades | `head -10 home/data/.../source/local/HomeDatabase.kt` (no `@Database` = still a wrapper) |

If any output diverges from this skill, update and date-stamp it.
