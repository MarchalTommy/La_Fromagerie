---
name: fromagerie-delivery-logistics-reference
description: Domain-knowledge pack for the delivery module of LaFromagerie — delivery zones/paths, selectable delivery dates, address geocoding (gouv APIs), Mapbox zone display, admin order preparation, route optimization (Google Routes vs OpenRouteService), delivery-day foreground tracking service, and the Room/Firestore path cache. Load this before touching code under delivery/, before debugging a delivery-zone/date/map bug, before working on admin/presentation delivery or preparation screens, or when asked "how does delivery routing work".
---

# LaFromagerie Delivery Logistics Reference

This is the domain-knowledge pack for everything delivery-related: how a customer picks a
delivery zone and date, how the admin edits zones, and how the admin executes a delivery day
with live route guidance. All facts verified 2026-07-06 against branch `claude/distracted-chaum-0986e4`.

## Jargon, defined once

| Term | Meaning here |
|---|---|
| **Delivery path** (a.k.a. "parcours", "tournée") | A named delivery zone/route: a set of (city, postcode) pairs, an assigned weekday, optional street-level restriction. Domain model `DeliveryPath`, Firestore collection `delivery_paths`. |
| **Path streets** | Optional per-path street allow-list (`DeliveryPath.streets`). If empty, the whole city is considered deliverable. Not currently enforced against the customer's typed address — see Gaps below. |
| **Preparation status** | Per-product, per-date checkbox state ("has this cheese been prepared for this delivery day yet") shown to the admin. Firestore collection `preparation_status`. Not the same as `Order.status`. |
| **Order status** | `OrderStatus` enum: `PENDING → PAID → IN_PREPARATION → PREPARED → IN_DELIVERY → DELIVERED`, or `CANCELED`. Lives on the `Order` domain model (`core/domain/.../Order.kt`), not on `DeliveryPath`. |
| **Route optimization** | Reordering today's delivery addresses into an efficient visiting order. Done by Google Routes at delivery-day execution time — **not** the same system that draws the road line on the zone map (that's OpenRouteService). |
| **GeoJSON** | The road-shaped line geometry drawn on the Mapbox map for a path. Fetched from OpenRouteService, stored as a JSON string in Room, rendered via Mapbox's `geoJsonSource`. |
| **`database_update` timestamp** | A Firestore cache-busting convention (see Data caching section) — not delivery-specific but delivery paths use it. |
| **Delivery-day tracking** | The admin foreground service that tracks GPS position during actual deliveries and pushes "next stop" notifications. Distinct from route optimization (which runs once at start) and from the zone map (client-facing, no GPS). |

## Client side: choosing a delivery zone and date

### Files

| Concern | File |
|---|---|
| ViewModel (shared client/admin) | `delivery/presentation/src/main/java/com/mtdevelopment/delivery/presentation/viewmodel/DeliveryViewModel.kt` |
| Client screen | `delivery/presentation/src/client/java/com/mtdevelopment/delivery/presentation/screen/DeliveryOptionScreen.kt` |
| Path picker dialog | `delivery/presentation/src/main/java/com/mtdevelopment/delivery/presentation/composable/DeliveryPathPickerComposable.kt` |
| Date picker + selectable-dates rules | `delivery/presentation/src/main/java/com/mtdevelopment/delivery/presentation/composable/DatePickerComposable.kt`, `.../model/ShippingSelectableDates.kt` |
| Map | `delivery/presentation/src/main/java/com/mtdevelopment/delivery/presentation/composable/MapBoxComposable.kt` |
| Localisation-type picker (GPS vs manual) | `delivery/presentation/src/main/java/com/mtdevelopment/delivery/presentation/composable/LocalisationTypePicker.kt` |
| User info fields + prefill | `delivery/presentation/src/main/java/com/mtdevelopment/delivery/presentation/composable/UserInfoComposable.kt`, `DeliveryViewModel.loadClientData()` |
| Path fetch/cache use case | `delivery/domain/src/main/java/com/mtdevelopment/delivery/domain/usecase/GetAllDeliveryPathsUseCase.kt` |
| Firestore path source | `delivery/data/src/main/java/com/mtdevelopment/delivery/data/source/remote/FirestoreDeliveryDataSource.kt` |
| Path repository (enrichment orchestration) | `delivery/data/src/main/java/com/mtdevelopment/delivery/data/repository/FirestorePathRepositoryImpl.kt` |
| Address geocoding (gouv) | `delivery/data/src/main/java/com/mtdevelopment/delivery/data/source/remote/AddressApiDataSource.kt`, `AddressApiRepositoryImpl.kt` |
| Address autocomplete (geopf) | `core/data/src/main/java/com/mtdevelopment/core/source/AutoCompleteApiDataSource.kt`, `AutocompleteRepositoryImpl.kt` |
| Road-geometry fetch (OpenRouteService) | `delivery/data/src/main/java/com/mtdevelopment/delivery/data/source/remote/OpenRouteDataSource.kt` |
| Room cache | `delivery/data/src/main/java/com/mtdevelopment/delivery/data/source/local/DeliveryDatabase.kt`, `dao/DeliveryDao.kt`, `model/entity/PathEntity.kt` |
| User-info DataStore | `core/data/src/main/java/com/mtdevelopment/core/local/SharedDatastoreImpl.kt` (`shared_settings`, key `user_information`) |

### The `delivery_paths` Firestore document, exactly as read

`FirestoreDeliveryDataSource.getAllDeliveryPaths()` reads each document in `delivery_paths` with these **exact field names** (`DataDeliveryPathsResponse`, `delivery/data/.../model/response/firestore/DataDeliveryPathsResponse.kt`):

| Firestore field | Kotlin property | Type |
|---|---|---|
| `path_name` | `path_name` | `String` |
| `cities` | `cities` | `List<String>` |
| `delivery_day` | `deliveryDay` | `String` (an `DayOfWeek` enum name, e.g. `"MONDAY"`) |
| `postcodes` | `postcodes` | `List<Int>` |
| `streets` | `streets` | `List<String>` (optional) |
| document id | `id` | used as-is |

`cities` and `postcodes` are zipped positionally (`path.cities zip path.postcodes`) — **the two arrays must stay the same length and in matching order** in Firestore, or city↔postcode pairing silently breaks.

### Path enrichment pipeline (`FirestorePathRepositoryImpl.getAllDeliveryPaths`)

1. Read raw path docs from Firestore (above).
2. For every city in every path, reverse-geocode `(cityName, zip)` → lat/lng via `AddressApiRepository.reverseGeocodeCity()` (gouv address API), in parallel per path (`async`/`await`).
3. If **any** city in a path fails to geocode, that whole path is dropped from the result (`mapNotNull` returning `null`). A path can silently disappear from the picker if one city's name/zip pair doesn't resolve — check this first if a path is "missing" for a customer.
4. If `withGeoJson = true` (admin map only, see below), fetch road-line geometry from OpenRouteService using the resolved city coordinates.
5. If **all** paths in the whole fetch failed to geocode, `onFailure` fires; otherwise the (possibly-partial) successful list is returned via `onSuccess`.

`getDeliveryPath(pathName)` (used to resolve a customer's previously-saved path name) does **not** geocode or fetch GeoJSON — it is a lighter partial reconstruction. **FIXED (2026-07-07, commit `58f85c9`, PR #43):** this method previously queried `whereEqualTo("pathName", …)` while the stored Firestore field is `path_name`, so the query could never match. The query key is now `path_name` and a regression test (`FirestoreDeliveryDataSourceTest`) guards it — see `fromagerie-firestore-data-model` §2.3.

### Address APIs — which does what

| API | Base host | Used for | Called from |
|---|---|---|---|
| `api-adresse.data.gouv.fr` (French government address API) | `ADDRESS_API_BASE_URL_WITHOUT_HTTPS` (`core/data/.../Constants.kt` and duplicated in `delivery/data/.../Constants.kt`) | Reverse-geocoding a path's cities to lat/lng (`/search/?q=<city>-<zip>&type=municipality`), and geocoding a typed address | `AddressApiDataSource` (delivery module) |
| `data.geopf.fr/geocodage` (French government geoplatform, IGN) | `AUTOCOMPLETE_API_BASE_URL_WITHOUT_HTTPS` (`core/data/.../Constants.kt`) | Live autocomplete suggestions as the user types an address (`/completion/?text=...&terr=25%2C39&poiType=zone d'habitation&type=StreetAddress&maximumResponses=3`) | `AutoCompleteApiDataSource` (core module, shared by client delivery form **and** admin manual-stop/path-city dialogs) |

Note the hardcoded `terr=25%2C39` in the autocomplete query — this restricts results to French department codes 25 and 39 (Doubs and Jura), matching the shop's real service area. **UNVERIFIED (as of 2026-07-06): whether this hardcoded restriction is intentional business logic or an oversight** — if the shop ever expands its delivery zone, this line silently keeps autocomplete scoped to those two departments regardless of what `delivery_paths` says.

### Selectable delivery dates (`ShippingSelectableDates.kt`)

Two `SelectableDates` implementations gate the Material3 `DatePicker`:

- **`ShippingSelectableDatesTest`** (name notwithstanding, this is the live production class — not a test) — used once a path is selected. A date is selectable only if:
  - its `DayOfWeek` matches `selectedPath.deliveryDay` (the path's assigned weekday), **and**
  - it is on/after `LocalDate.now().plusDays(2)` (a fixed 2-day lead time, computed once at class-load as a top-level `val limitDate`, not re-evaluated per app-open across a midnight boundary within the same process).
- **`ShippingDefaultSelectableDates`** — used when no path is selected yet; rejects every date (`isSelectableDate` always `false`), acting as a "pick a path first" gate.
- `isSelectableYear` in both allows the current year and next year only.

`getDatePickerState()` (`DatePickerComposable.kt`) additionally pre-computes the list of valid dates for the rest of the calendar year by brute-force scanning `dayOfYear + 2 .. 365` and checking each against the selectable-dates rule, then seeds `initialSelectedDateMillis` with the first hit. If no path is selected, it falls back to "tomorrow" as the initial (non-selectable) display date. All calendars use `Locale.FRANCE`.

**Gotcha:** the 2-day lead time and the weekday match are the *only* rules — there's no admin-configurable "blackout dates" or max-orders-per-day cap in this picker. UNVERIFIED (2026-07-06): whether such a cap exists anywhere in the order-creation path.

### Mapbox zone display

`MapBoxComposable.kt` renders paths as colored line layers (`pathsColors`, 5 hardcoded hex colors cycling by path index) on a `MapboxMap`. Key facts:

- Requires `MAPBOX_PUBLIC_TOKEN` (BuildConfig field, `delivery/presentation` module) to be set to a real value before `MapboxOptions.accessToken` is assigned in both `DeliveryOptionScreen` variants (client and admin). If this bakes as the literal string `"null"` (non-interactive build with no env var — see `fromagerie-config-and-secrets`), the map renders blank/broken with no explicit error surfaced to the user.
- `MAPBOX_SECRET_TOKEN` (different secret, used only in `settings.gradle.kts` for the Mapbox Maven repository credentials) is **not** the same secret as `MAPBOX_PUBLIC_TOKEN` — one authenticates the Gradle dependency download, the other authenticates the SDK at runtime. Missing either breaks a different thing (build-time 401 vs. runtime blank map).
- Camera: shows an overview of all paths' bounding box by default; flies to a single path's bounds when one is chosen; flies to the user's geocoded location when available and no path is chosen yet.
- Path geometry source: `path.geoJson` (a `GeoJsonFeatureCollection`), serialized to Mapbox `FeatureCollection.fromJson(...)`. If `geoJson` is null (OpenRouteService fetch failed or wasn't requested), that path draws no line — no fallback straight-line rendering.

### Localisation-type picker

`LocalisationTypePicker.kt` shows an "auto-locate" button only when: no path selected yet, localisation not already acquired, and `Geocoder.isPresent()` (device has a reverse-geocoder — most do, but not guaranteed on all AOSP forks). Tapping it triggers `shouldAskLocalisationPermission`, which is wired up through `PermissionComposable`/`PermissionManagerComposable` (runtime location permission flow) — not detailed further here as it's standard Android permission plumbing.

### User-info persistence and prefill

`shared_settings` DataStore (`core/data/.../SharedDatastoreImpl.kt`), key `user_information`, stores a Gson-serialized `UserInformationData` with: `name`, `address`, `billingAddress`, `lastSelectedPath` (the path **name**, not id). On `loadClientData()`, `DeliveryViewModel` prefills the name/address fields and re-resolves `lastSelectedPath` against the freshly-fetched path list by name match — if the shop later renames a path, previously-saved customers silently lose their prefilled selection (name changed → no match → `selectedPath = null`).

## Admin side: editing paths

### Path editing (`PathEditDialog.kt`, `delivery/presentation/src/admin/...`)

- Edits an `AdminUiDeliveryPath` (id, name, cities as `List<Pair<String,Int>>`, deliveryDay, streets).
- New city added via `CityPostalCodeAutocompleteTextField` — backed by the same geopf autocomplete API described above.
- Cities are manually reorderable (up/down arrows) and swipe-to-delete (`SwipeToDismissBox`, start-to-end only).
- `deliveryDay` is a single `FilterChip` selection over `DayOfWeek.entries` — **only one weekday per path** (matches `DeliveryPath.deliveryDay: String`, a single value not a set).
- Validate button is only enabled when name is non-blank, cities non-empty, and deliveryDay non-blank.
- On confirm, `DeliveryOptionScreen` (admin variant) decides add-vs-update by checking whether `newPath.id` already exists in the currently loaded path list, then calls `AdminViewModel.addNewDeliveryPath` / `updateDeliveryPath` / `deleteDeliveryPath`, which go through `FirebaseAdminRepositoryImpl` → `FirestoreAdminDatasource` → the same `delivery_paths` collection (`admin/data/src/main/java/com/mtdevelopment/admin/data/source/FirestoreAdminDatasource.kt`).
- After any add/update/delete, the screen calls `deliveryViewModel.loadAdminData(forceRefresh = true)` to bypass the Room cache and re-read Firestore immediately — see Data caching below for why `forceRefresh` matters here.

## Admin side: order preparation (`OrderPreparationScreen.kt`)

Purpose: help the admin know how much of each product to prepare, grouped by delivery date, **not** to prepare the actual delivery route.

- Groups all orders by `deliveryDate` (a formatted string, sorted via `toTimeStamp()`, newest first).
- For each date, aggregates quantities across orders into `Map<productName, totalQuantity>` — e.g. two orders each wanting 2 units of "Comté" show up as one row with quantity 4.
- Each product row is expandable to show the per-customer breakdown (name, quantity, order note).
- Each row has a checkbox that writes a `PreparationStatus` via `onUpdateStatus`:
  - `id` is derived as `"<deliveryDate-without-slashes>_<productName-without-spaces>"` (e.g. delivery date `"05/07/2026"` + product `"Comté AOP"` → id `"05072026_ComtéAOP"`). This is a **generated composite key**, not a Firestore auto-id — collisions are possible if two different product names normalize to the same no-space string on the same date (rare but not impossible with punctuation-only differences).
  - Persisted to the `preparation_status` Firestore collection (see `admin/data/.../FirestoreAdminDatasource.kt` for the write path — same collection name pattern as other admin writes).
- Past delivery dates (`deliveryDate.toLocalDate().isBefore(today)`) are rendered faded/muted but not hidden — history stays visible.

This is **preparation** tracking (has the cheese been readied), separate from `OrderStatus` (the lifecycle field on `Order` itself, which this screen does not mutate).

## Admin side: delivery-day execution

### Workflow, start to finish

| Step | What happens | Where |
|---|---|---|
| 1. Open Delivery Helper screen | Loads all orders, filters to `deliveryDate == today` (compared as epoch millis at UTC midnight) | `DeliveryHelperScreen.kt`, `viewModel.getAllOrders()` |
| 2. Check permissions | Location (always) + POST_NOTIFICATIONS (API 33+) checked on entry and re-checked on `ON_RESUME` via a `LifecycleEventObserver` | `DeliveryHelperScreen.kt` |
| 3. (Optional) Add manual stop | `DeliveryAddDialog` lets the admin add an ad-hoc delivery address (e.g. phone order) via the same geopf autocomplete, feeding `AdminViewModel.addOrder()` | `admin/presentation/.../composable/DeliveryAddDialog.kt` |
| 4. Tap "Démarrer la livraison" | `startDelivery()`: gets current GPS fix, calls `AdminViewModel.getOptimisedPath()` (→ `GetOptimizedDeliveryUseCase` → Google Routes), builds a `https://www.google.com/maps/dir/<lat,lng>/.../` multi-stop URL, opens Google Maps app, and starts the foreground tracking service | `DeliveryHelperScreen.kt: startDelivery()` |
| 5. Foreground service runs | `DeliveryTrackingService` re-fetches today's orders, re-requests the optimized route (independently of step 4's request — see Route optimization below), then streams device location and pushes "next stop" notifications | `app/src/admin/.../DeliveryTrackingService.kt` |
| 6. Stop delivery | Admin taps "Arrêter la livraison" button (only shown once `isInTracking` is true) → `stopDeliveryTracking()` → `Intent` `stopService()` on `DeliveryTrackingService`, or taps "Arrêter le suivi" action on the persistent notification → `NotificationBroadcastReceiver` stops the service and cancels the notification | `DeliveryHelperScreen.kt`, `NotificationBroadcastReceiver.kt` |

**Correction to a common assumption:** there is no automatic "stop tracking when the app comes back to foreground" logic in the current code — despite commit `f272679`'s message ("Added a way to stop the delivery when coming back to the app"), the actual mechanism today is a **manual button** driven by the persisted `isInTrackingMode` flag (`AdminDatastorePreferenceImpl`, key `is_in_tracking_mode`, in the `admin_data` DataStore), not a lifecycle callback. Re-verify with the grep command below if this matters for a bug you're chasing — the git history and the present code disagree on the mechanism's automaticity, and only the code is ground truth.

### Route optimization: Google Routes vs OpenRouteService — do not confuse them

Two completely different services are used for two completely different jobs:

| | **Google Routes** (`routes.googleapis.com`) | **OpenRouteService** (`api.openrouteservice.org`) |
|---|---|---|
| Used for | Delivery-day **stop ordering** (which address to visit in what sequence) | Zone-map **road-line geometry** (the line drawn on the Mapbox map for a path) |
| Called by | `GoogleRouteRepositoryImpl` → `GoogleRouteDataSource`, from `GetOptimizedDeliveryUseCase` | `OpenRouteDataSource`, from `FirestorePathRepositoryImpl` (only when `withGeoJson=true`) |
| Secret | `GOOGLE_API` (admin/data BuildConfig) | `OPEN_ROUTE_TOKEN` (delivery/data BuildConfig) |
| Output consumed by | `DeliveryHelperScreen` (build Google Maps deep link) and `DeliveryTrackingService` (compute next stop) | `MapBoxComposable` (render polyline) |
| Result caching | `AdminDatastorePreference.dailyDeliveryPathGeocodedFlow` — cached route reused only if the cached order-id set exactly matches today's order-id set (`GetOptimizedDeliveryUseCase`) | Cached inside the `PathEntity.geojson` string column in Room |

**`GetOptimizedDeliveryUseCase` mapping invariant** (documented in code, `DetermineNextDeliveryStopUseCase.kt` KDoc): `GoogleRouteRepositoryImpl` builds `optimizedRoute` from each route leg's **end location**, so `waypoints[i]` corresponds 1:1 to `orders[i]` — except the very last leg, whose end location is the return-to-shop leg with no matching order. `DetermineNextDeliveryStopUseCase.findNextClosestOrder` relies on this by bounding `i < orders.size`.

`reorderList` (used by `GoogleRouteRepositoryImpl` to reorder `dailyOrders` per Google's `optimizedIntermediateWaypointIndex`) validates indices (size, bounds, duplicates) and **falls back to the original, unoptimized order** if Google's response doesn't cleanly map — e.g. when several orders share the same address. This means a "route wasn't actually optimized" bug can be silent: no error, just orders in creation order instead of geographic order.

### Next-stop determination (`DetermineNextDeliveryStopUseCase`)

Given current GPS location and the cached optimized route, walks the waypoint list looking for the first waypoint still more than `AT_STOP_THRESHOLD_METERS = 50.0f` meters away (via `android.location.Location.distanceBetween`), and returns the order mapped to that index. Returns `null` once every waypoint has been "reached" (service then stops tracking and stops itself, see table above).

### Foreground service and notifications — known fragility

`DeliveryTrackingService` (`app/src/admin/java/com/mtdevelopment/lafromagerie/DeliveryTrackingService.kt`) and `NotificationBroadcastReceiver` (same directory) are **admin-flavor-only** source-set files (not present under `app/src/client/`).

- Commit `83b4ff2` ("Started working on a notification based helper... I used AI here to help me with the service as I'm not used to do it, but as always, I spend more time debugging it than using it... It wrote too verbose code") — first pass, admittedly fragile from the start.
- Commit `f272679` ("Finalized the notification system... it was HARD as I used to know how to manage notifications but it was a while ago, I relearned everything") — hardening pass.
- Remote branch `origin/fix/admin_delivery_instability` exists — evidence of at least one dedicated stabilization effort; treat as a signal that this subsystem has broken in the field before. Inspect that branch's diff before re-implementing tracking/notification logic from scratch.
- `startForeground()` **must** be called within 5 seconds of `onStartCommand` on Android O+ (the code does this immediately via `createInitialNotification()`, before any network calls) — if you add blocking work before that call, expect an `ANR`/`ForegroundServiceDidNotStartInTimeException` crash.
- Service returns `START_STICKY` — the OS may restart it after being killed, with a `null` intent, which the `onStartCommand` handles by re-running `fetchOrdersAndRoute()` — meaning a killed-and-restarted service **starts the whole route calculation over**, not resume mid-route.
- Notification channel `delivery_tracking_channel`, notification id `123` (both hardcoded constants) — if you add a second foreground-notification feature, don't reuse `123`.

## Data caching: Room + `database_update` invalidation

`GetAllDeliveryPathsUseCase` decides whether to hit Firestore or read the local Room cache:

1. Check `sharedDatastore.shouldRefreshPaths` (a `Boolean` in the `shared_settings` DataStore, key `should_refresh_paths`, **defaults to `true`** if never set).
2. If `true` (or `forceRefresh` passed by the caller): fetch from Firestore, persist every path to Room (`RoomDeliveryRepository.persistPath`), delete any local Room path whose id no longer appears in the fresh Firestore list (garbage-collect deleted paths), then flip the flag to `false`.
3. If `false`: read straight from Room (`DeliveryDatabase` → `DeliveryDao`, table `paths`), no network call at all.
4. On Firestore failure during step 2: the flag is explicitly **re-set to `true`** so the next attempt retries from network rather than silently falling back to (possibly empty) Room.

`shouldRefreshPaths` itself is flipped back to `true` by a **separate, unrelated mechanism**: `GetLastFirestoreDatabaseUpdateUseCase` (`home/domain/.../GetLastFirestoreDatabaseUpdateUseCase.kt`), which runs at app/home-screen load and compares two server-side timestamps against locally-stored ones:

- Firestore collection `database_update`, document id `path_timestamp`, field `last_update` (a Firestore `Timestamp`) — compared against `sharedDatastore.lastFirestorePathsUpdate` (`shared_settings` key `firestore_path_update`).
- Document id `products_timestamp` does the equivalent for the unrelated products cache (`fromagerie-firestore-data-model` territory, not delivery).
- Mismatch (or locally-stored value `== 0L`, i.e. never set) → `setShouldRefreshPaths(true)`.

**Practical consequence:** editing a delivery path in the admin app does **not** by itself cause an already-cached client to refresh mid-session — other devices pick up the change once `database_update/path_timestamp` is bumped and the app re-checks it at next home-screen load. **Who bumps it (verified 2026-07-07):** the admin flavor itself — `admin/data/src/main/java/com/mtdevelopment/admin/data/source/FirestoreAdminDatasource.kt` `saveNewDatabasePathsUpdate()` (lines ~140-148) does `collection("database_update").document("path_timestamp").set(mapOf("last_update" to Timestamp(...)))` after every path write, and `saveNewDatabaseProductUpdate()` (lines ~88-92) does the equivalent for `products_timestamp`. Both are invoked from `FirebaseAdminRepositoryImpl`. See `fromagerie-firestore-data-model` §4 for the full write path. If a "path changes aren't showing up" bug appears, first check whether the admin write actually succeeded (the `Result.failure` branch is silent) before suspecting the client cache logic.

### Room schema (`PathEntity`, table `paths`)

| Column (`@SerialName`) | Kotlin type | Notes |
|---|---|---|
| `id` | `String` | primary key |
| `name` | `String` | maps to `DeliveryPath.pathName` |
| `cities` | `Map<String, Int>` | city name → postcode; **note this is a Map in Room, but a `List<Pair<String,Int>>` in the domain model** — order is not guaranteed to round-trip identically through a Map |
| `locations` | `List<Coordinate>` | geocoded lat/lng centers |
| `delivery_day` | `String` | |
| `geojson` | `String` | the whole `GeoJsonFeatureCollection` JSON-encoded as one text blob |

The `cities: Map<String, Int>` vs. domain `List<Pair<String, Int>>` mismatch means **city ordering set in `PathEditDialog`'s manual reorder UI is not guaranteed to survive a Room round-trip** — reordering only matters for the in-memory session and for what's written straight back to Firestore, not for what comes back out of the local cache. UNVERIFIED (as of 2026-07-06): whether this has caused an observed bug; flagged here as a latent one from static reading of `PathEntity.kt`.

## When NOT to use this skill

- Payment/checkout flow downstream of delivery selection → **fromagerie-payments-reference**
- Firestore schema for `products`, `orders` beyond what's noted here → **fromagerie-firestore-data-model**
- Secrets/BuildConfig wiring for `MAPBOX_*`, `OPEN_ROUTE_TOKEN`, `GOOGLE_API` → **fromagerie-config-and-secrets**
- Build/Gradle/env setup → **fromagerie-build-and-env**
- Symptom-first triage ("map is blank", "notification stuck") → **fromagerie-debugging-playbook** (once written), come back here for mechanics
- Historical narrative of the notification/foreground-service pain and other reverts → **fromagerie-failure-archaeology**
- Running/installing the admin flavor on a device → **fromagerie-run-and-operate**
- General clean-architecture module layout conventions → **fromagerie-architecture-contract**

## Provenance and maintenance

All facts verified 2026-07-06 against the working tree (branch `claude/distracted-chaum-0986e4`). Re-verify drift-prone claims:

- Firestore field names for `delivery_paths`: `grep -n "path_name\|delivery_day\|postcodes" delivery/data/src/main/java/com/mtdevelopment/delivery/data/source/remote/FirestoreDeliveryDataSource.kt`
- `database_update` document ids/fields: `grep -n "products_timestamp\|path_timestamp\|last_update" home/data/src/main/java/com/mtdevelopment/home/data/source/remote/FirestoreDatabase.kt`
- Admin still writes `database_update` timestamps: `grep -n "path_timestamp\|products_timestamp" admin/data/src/main/java/com/mtdevelopment/admin/data/source/FirestoreAdminDatasource.kt` (expect 2 hits — the `.set(` writers for both documents)
- Google Routes vs OpenRouteService split still holds: `grep -rn "GOOGLE_ROUTE_BASE_URL\|OPEN_ROUTE_BASE_URL" core/data/src/main/java/com/mtdevelopment/core/data/Constants.kt`
- MapBox public vs secret token distinction: `grep -n "MAPBOX_PUBLIC_TOKEN" delivery/presentation/src/client/java/com/mtdevelopment/delivery/presentation/screen/DeliveryOptionScreen.kt delivery/presentation/src/admin/java/com/mtdevelopment/delivery/presentation/screen/DeliveryOptionScreen.kt` and `grep -n "MAPBOX_SECRET_TOKEN" settings.gradle.kts`
- Selectable-dates lead time still 2 days: `grep -n "limitDate\|plusDays" delivery/presentation/src/main/java/com/mtdevelopment/delivery/presentation/model/ShippingSelectableDates.kt`
- Stop-tracking still manual (not automatic on foreground): `grep -n "isInTrackingMode\|ON_RESUME\|ON_STOP" admin/presentation/src/main/java/com/mtdevelopment/admin/presentation/screen/DeliveryHelperScreen.kt app/src/admin/java/com/mtdevelopment/lafromagerie/MainActivity.kt`
- Foreground-service fragility branch still present: `git branch -a | grep admin_delivery_instability`
- Preparation-status composite id format: `grep -n "statusId =" admin/presentation/src/main/java/com/mtdevelopment/admin/presentation/screen/OrderPreparationScreen.kt`
- `PathEntity` cities Map-vs-List mismatch: `grep -n "availableCities: Map" delivery/data/src/main/java/com/mtdevelopment/delivery/data/model/entity/PathEntity.kt`
- Autocomplete hardcoded department restriction: `grep -n "terr=" core/data/src/main/java/com/mtdevelopment/core/source/AutoCompleteApiDataSource.kt`
