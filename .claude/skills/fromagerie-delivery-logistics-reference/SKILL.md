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
| **Delivery path** (a.k.a. "parcours", "tournée") | A named delivery zone/route: a list of `DeliveryCity` (name, postcode, optional street allow-list) plus an assigned weekday and frequency. Domain model `DeliveryPath`, Firestore collection `delivery_paths`. |
| **City streets** | Optional street allow-list on a **(path, city) pair** — `DeliveryCity.streets` (`core/domain/.../model/DeliveryCity.kt`), not a per-path field. Empty (the common case) means the path serves the whole city. Enforced against the customer's address by `DetermineDeliveryEligibilityUseCase`. |
| **Split city** | A city listed by two paths, each with a different street allow-list, so the two tournées share it. The real case: Boujailles, on the Tuesday and Friday paths. |
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
| Date picker | `delivery/presentation/src/main/.../composable/DatePickerComposable.kt` (rendering) + `delivery/domain/.../usecase/BuildSelectableDeliveryDatesUseCase.kt` (the rules). There is **no path picker**: the customer picks a tournée by picking a date. |
| Map | `delivery/presentation/src/main/java/com/mtdevelopment/delivery/presentation/composable/MapBoxComposable.kt` |
| Localisation-type picker (GPS vs manual) | `delivery/presentation/src/main/java/com/mtdevelopment/delivery/presentation/composable/LocalisationTypePicker.kt` |
| User info fields + prefill | `delivery/presentation/src/main/java/com/mtdevelopment/delivery/presentation/composable/UserInfoComposable.kt`, `DeliveryViewModel.loadClientData()` |
| Path fetch/cache use case | `delivery/domain/src/main/java/com/mtdevelopment/delivery/domain/usecase/GetAllDeliveryPathsUseCase.kt` |
| Firestore path source | `delivery/data/src/main/java/com/mtdevelopment/delivery/data/source/remote/FirestoreDeliveryDataSource.kt` |
| Path repository (enrichment orchestration) | `delivery/data/src/main/java/com/mtdevelopment/delivery/data/repository/FirestorePathRepositoryImpl.kt` |
| Address geocoding + street suggestions (gouv) | `delivery/data/src/main/java/com/mtdevelopment/delivery/data/source/remote/AddressApiDataSource.kt`, `AddressApiRepositoryImpl.kt` |
| Address autocomplete (geopf) | `core/data/src/main/java/com/mtdevelopment/core/source/AutoCompleteApiDataSource.kt`, `AutocompleteRepositoryImpl.kt` |
| Road-geometry fetch (OpenRouteService) | `delivery/data/src/main/java/com/mtdevelopment/delivery/data/source/remote/OpenRouteDataSource.kt` |
| Room cache | `delivery/data/src/main/java/com/mtdevelopment/delivery/data/source/local/DeliveryDatabase.kt`, `dao/DeliveryDao.kt`, `model/entity/PathEntity.kt` |
| User-info DataStore | `core/data/src/main/java/com/mtdevelopment/core/local/SharedDatastoreImpl.kt` (`shared_settings`, key `user_information`) |

### The `delivery_paths` Firestore document, exactly as read

`FirestoreDeliveryDataSource.getAllDeliveryPaths()` reads each document in `delivery_paths` with these **exact field names** (`DataDeliveryPathsResponse`, `delivery/data/.../model/response/firestore/DataDeliveryPathsResponse.kt`):

| Firestore field | Kotlin property | Type |
|---|---|---|
| `path_name` | `path_name` | `String` |
| `delivery_day` | `deliveryDay` | `String` (a `DayOfWeek` enum name, e.g. `"MONDAY"`) |
| `delivery_frequency` | `deliveryFrequency` | `String`, defaults `"WEEKLY"` |
| `city_entries` | `cityEntries` | `List<DataDeliveryCityResponse>` — **canonical**; each has `city`, `postcode`, `streets` |
| `cities` | `cities` | `List<String>` — legacy parallel array |
| `postcodes` | `postcodes` | `List<Int>` — legacy parallel array |
| document id | `id` | used as-is |

`toDeliveryCities()` turns the document into `List<DeliveryCity>`: it uses `city_entries` when
present and non-empty, otherwise falls back to zipping `cities`/`postcodes` into unrestricted
cities. **Documents predating the split-city work keep working unchanged** and convert on the
next admin save. Full contract, including why the old path-level `streets` field is never
written any more: `fromagerie-firestore-data-model` §2.3.

On the legacy fallback only, `cities` and `postcodes` are zipped positionally — **the two arrays
must stay the same length and in matching order**, or city↔postcode pairing silently breaks.
`city_entries` exists to retire that hazard.

### Path enrichment pipeline (`FirestorePathRepositoryImpl.getAllDeliveryPaths`)

1. Read raw path docs from Firestore (above).
2. For every city in every path, reverse-geocode `(cityName, zip)` → lat/lng via `AddressApiRepository.reverseGeocodeCity()` (gouv address API), in parallel per path (`async`/`await`).
3. If **any** city in a path fails to geocode, that whole path is dropped from the result (`mapNotNull` returning `null`). A path can silently disappear from the picker if one city's name/zip pair doesn't resolve — check this first if a path is "missing" for a customer.
4. If `withGeoJson = true` (admin map only, see below), fetch road-line geometry from OpenRouteService using the resolved city coordinates.
5. **An empty result is always reported as `onFailure`** (changed 2026-07-29). Whether the emptiness comes from Firestore returning no document, from every path being dropped at step 3, or from every document resolving to zero cities (neither `city_entries` nor the legacy arrays), "zero paths" is never a legitimate answer — the shop always has at least one. Reporting success on an empty list used to poison the cache permanently; see fromagerie-failure-archaeology §14 before relaxing this. A **partial** result (some paths dropped, at least one survivor) is still returned via `onSuccess`, unchanged.

> **Firestore offline trap, worth internalizing:** `collection(...).get()` with the default
> source does **not** fail when the device is offline. It resolves from Firestore's own local
> cache and completes *successfully* with whatever is there — including zero documents on a
> first launch. `addOnFailureListener` never fires. Any read whose emptiness has consequences
> must be checked explicitly; a successful `get()` is not evidence that the server answered.

`getDeliveryPath(pathName)` (used to resolve a customer's previously-saved path name) does **not** geocode or fetch GeoJSON — it is a lighter partial reconstruction. **FIXED (2026-07-07, commit `58f85c9`, PR #43):** this method previously queried `whereEqualTo("pathName", …)` while the stored Firestore field is `path_name`, so the query could never match. The query key is now `path_name` and a regression test (`FirestoreDeliveryDataSourceTest`) guards it — see `fromagerie-firestore-data-model` §2.3.

### Matching an address to a path (`DetermineDeliveryEligibilityUseCase`)

`delivery/domain/src/main/java/com/mtdevelopment/delivery/domain/usecase/DetermineDeliveryEligibilityUseCase.kt`
— the **single** decision point for "which tournée serves this address, if any". It is a pure
function over already-geocoded inputs (`paths`, `userCity`, `userStreet`, `addressText`,
`userLocation`), so it holds no Android `Geocoder`.

Callers do the geocoding, then delegate:

| Flow | Caller | Where the street comes from |
|---|---|---|
| Typed / autocompleted address | `CustomerContent.kt` (`delivery/presentation/src/main`) | `Address.thoroughfare` on the manual branch; **null on the autocomplete branch, which never runs the geocoder** — the street has to be recognised inside `addressText` |
| GPS "locate me" | `PermissionManagerComposable.kt` (same module) | `Address.thoroughfare` from reverse geocoding |

Each (path, city) pair whose city matches is binned into a tier; the most specific non-empty
tier wins, and **everything tied at the top of it is returned**:

| Situation, for one city across N paths | Result |
|---|---|
| The customer's street is listed on exactly one path | that path |
| Listed on several paths, labels of different length | **longest label wins** (see below) |
| Listed on several paths, labels equally specific | **all of them** → customer chooses |
| No street match, ≥1 path covers the city unrestricted | that path; **several → customer chooses** |
| No street match, **every** path restricts the city (including N=1) | `STREET_NOT_COVERED` |
| No path covers the city, within `MAX_DISTANCE_FOR_PICKUP_METERS` (5 km, defined in this file) | `ASK_FOR_SUPPORT` |
| Beyond that | `NOT_ELIGIBLE` |

A street match always beats a whole-city match: the shop went to the trouble of naming that
street, so it is the more deliberate answer.

`DeliveryEligibilityResult.candidatePaths` holds every winner in path order; `matchingPath` is
just its first entry, kept for callers that only need one. **Nothing resolves a tie by list
order** — that order comes from Firestore document iteration, so silently taking the first would
hand the same customer a different tournée from one session to the next. It also returns
`resolvedCity`/`resolvedLocation`, recovered from the matched city when geocoding supplied
neither.

**Longest-label-wins exists for one concrete case.** Street labels are matched loosely inside the
address text, so an address on "Rue du Moulin Neuf" also contains "Rue du Moulin"; if the two
belong to different paths, both match. The longer label is the street the customer lives on. The
reverse never collides — the longer label is simply absent from the shorter address — so this
only fires on the ambiguous direction.

**`STREET_NOT_COVERED` is a product decision, not a fallback**, and it applies even when only one
path restricts the city. A street list is a deliberate statement that the rest of the commune is
not served, so there is nothing safe to fall back on. The UI says so explicitly
(`auto_geoloc_street_not_covered`) and offers a support-request email asking for the street to be
added. The fix loop is the admin adding the street to the right city group. Do not replace this
with "pick the first path" without Tommy's call.

⚠️ **The uncovered-street button is not gated on proximity.** `streetNotCovered` is its own
condition in `CustomerContent.kt`, because it is the one verdict where the city *is* served. It
used to ride on `userLocationCloseFromPath`, and the typed-address flow set the legacy booleans
by hand instead of calling `updateEligibility` — so on the commonest flow the flag was never
raised and the customer saw **no button at all**, neither "Continuer" nor the support request.
Both flows now go through `DeliveryViewModel.updateEligibility`. Fixed in PR #60.

⚠️ **This logic used to be a private function duplicated verbatim between the two Composables
above**, which is why a bug in it survived two months unnoticed: it treated a street list as a
**path-level** filter, so any path restricting one of its cities became unusable for all its
other cities (configuring Boujailles killed Frasne and Courvière). Unified and unit-tested in
PR #59 — `DetermineDeliveryEligibilityUseCaseTest` encodes the real shop configuration. **If you
add a third entry point, call the use case; do not copy the matching loop again.**

### Address APIs — which does what

| Endpoint | Used for | Called from |
|---|---|---|
| `/geocodage/search/?q=<city>-<zip>&type=municipality` | Reverse-geocoding a path's cities to lat/lng, and geocoding a typed address | `AddressApiDataSource` (delivery module) |
| `/geocodage/search/?q=<query> <city>&type=street&postcode=<zip>&limit=10` | Street-name suggestions while the admin restricts a path to part of a commune | `AddressApiDataSource.getStreetsInCity` |
| `/geocodage/completion/?text=...&terr=25%2C39&poiType=zone d'habitation&type=StreetAddress&maximumResponses=3` | Live autocomplete as the user types a city or address | `AutoCompleteApiDataSource` (core module, shared by client delivery form **and** the admin path editor) |

All three go to `data.geopf.fr`, the Géoplateforme. **Migrated 2026-07-30** (frontier §3b, now
resolved) — the previous host, `api-adresse.data.gouv.fr`, was sunset on 31/01/2026 and had been
answering only by proxying. The endpoints are identical apart from the `/geocodage` path prefix;
the response DTOs (`AddressData`, `AutoCompleteSuggestions`) were unchanged by the move.

Two things to keep straight when editing these calls:

- **The host constant is duplicated** — `core/data/.../Constants.kt` (autocomplete) and
  `delivery/data/.../model/Constants.kt` (address, city, street). Both must change together;
  `GeocodingHostTest` in `delivery/data` fails if they drift.
- **The `/geocodage` prefix belongs in `encodedPath`, never in the host.** Ktor's
  `URLBuilder.host` takes an authority — a slash in it yields a request that never arrives. This
  is exactly how the old `AUTOCOMPLETE_API_BASE_URL_WITHOUT_HTTPS = "data.geopf.fr/geocodage"`
  was malformed. Hence the separate `GEOCODAGE_PATH_PREFIX` constant.

Note the hardcoded `terr=25%2C39` in the autocomplete query — this restricts results to French department codes 25 and 39 (Doubs and Jura), matching the shop's real service area. **UNVERIFIED (as of 2026-07-06): whether this hardcoded restriction is intentional business logic or an oversight** — if the shop ever expands its delivery zone, this line silently keeps autocomplete scoped to those two departments regardless of what `delivery_paths` says.

**The street lookup puts the commune in `q`, not just in `postcode`.** A postcode covers several
communes here — 25560 is Frasne, Boujailles *and* Courvière — and the API ranks on query text
alone, so `q=rue&postcode=25560` returns ten Frasne streets and nothing for Boujailles. The
repository filters on the commune again afterwards (the search stays fuzzy), which meant the
postcode-only version returned an empty list every time while looking perfectly correct in unit
tests. Found by running it on a device, not by reading it.

### Selectable delivery dates (`BuildSelectableDeliveryDatesUseCase`)

`delivery/domain/src/main/java/com/mtdevelopment/delivery/domain/usecase/BuildSelectableDeliveryDatesUseCase.kt`
— takes **every path that serves the customer** plus an injected `now`, and returns the tiles the
date dialog renders:

```kotlin
data class SelectableDeliveryDate(date, pathId, pathName, isPastDeadline)
operator fun invoke(paths: List<DeliveryPath>, now: LocalDateTime, limit: Int = 4)
```

Per path: the next `limit` occurrences of `deliveryDay`, filtered by `deliveryFrequency`
(`WEEKLY` / `BIWEEKLY_EVEN` / `BIWEEKLY_ODD`, week parity on `WeekFields.of(Locale.FRANCE)`,
falling back to `FRIDAY` if the day string does not parse). Then merged across paths, sorted,
**deduplicated by date**, cut to `limit` distinct dates. Orders close the day before at
`ORDER_CUTOFF_HOUR` (12:00); past-cutoff dates are still listed but not selectable, because
hiding them would make the list silently shift.

**This is how the customer picks a tournée.** When `candidatePaths` holds more than one path the
tiles carry a path-name chip and the chosen date assigns the path — there is no separate picker.
`DatePickerComposable` takes `paths: List<UiDeliveryPath>` and calls back with
`(epochMillis, path)`; the client screen applies the path before persisting. With one path the
chip is hidden and the behaviour is identical to before.

Same-day dedup is deliberate: the order records only the date (see "no path id on the order"
below), so two tournées delivering the same day are indistinguishable downstream and offering
the day twice would read as a bug.

**Gotcha:** the weekday, the frequency and the cutoff are the *only* rules — no admin-configurable
blackout dates, no max-orders-per-day cap. UNVERIFIED (2026-07-06): whether such a cap exists
anywhere in the order-creation path.

**Deleted 2026-07-30 — do not resurrect from git history.**
`delivery/presentation/.../model/ShippingSelectableDates.kt` (`ShippingSelectableDatesTest`, a
production class despite the name, plus `ShippingDefaultSelectableDates`) encoded a **2-day lead
time that was never the live rule**. It gated the Material3 `DatePicker` that the custom tile dialog
replaced, and had had zero callers since. The live rule is the J-1 noon cutoff above, and it lives in
`BuildSelectableDeliveryDatesUseCase` — nowhere else. Same commit removed the equally dead
`DeliveryPathPickerComposable.kt` (`radioOptions[0]` on a possibly-empty list) and the write-only
`DeliveryUiDataState.dateFieldText` / `.shouldDatePickerBeClickable` fields with their setters.

### The order records the date, not the path

`Order` (`core/domain/.../model/Order.kt`) carries `deliveryDate: String` (`dd/MM/yyyy`) and **no
path id**. The tournée is a client-side concept only; the closest thing to persistence is
`lastSelectedPath` (by *name*) in DataStore, which exists to prefill the form, not to drive
fulfilment.

This was a deliberate call (owner, 2026-07-30) when the multi-path choice was added: adding a
field to `orders` is a schema change, and the date already identifies the tournée in every real
configuration. **The contrapositive is the thing to remember:** if two paths ever share the same
`deliveryDay` + `deliveryFrequency`, a customer served by both produces the same date either way,
the choice carries no information, and the admin cannot tell which route they belong to. That is
why the merged date list deduplicates by date rather than showing the day twice. If the shop ever
wants two same-day tournées, this invariant is what breaks first — revisit it before adding them,
not after.

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

### Path editing (`PathEditScreen.kt`, `delivery/presentation/src/admin/...`)

A full-screen destination since PR #61 — it replaced `PathEditDialog.kt` (deleted), which crammed
identity, schedule, cities and a comma-separated street field into one scrolling card.

- **Route:** `PathEditScreenDestination(pathId: String?)` in `app/src/main/.../navigation/CheeseScreens.kt`,
  registered **only** in the admin `NavGraph`. `pathId == null` means create; the list screen tells
  the two apart by checking whether the tapped card's id is in the loaded path list (the carousel
  prepends a synthetic "Ajouter un parcours" card whose id is not).
- **No `Scaffold`** — the admin app has exactly one, at the activity level. That top bar's
  action icon is suppressed on this route (`MainActivity.kt`, prefix match since the route carries
  an argument): a shortcut elsewhere on a form with unsaved edits only invites losing them.
- **Draft state** is a `@Serializable PathDraft` in `rememberSaveable` behind a JSON saver — no
  ViewModel, no Koin definition, and it survives rotation and process death. Every mutation is a
  pure function in `PathDraft.kt` (`withCityAdded`, `withCityRemovedAt`, `withCityMoved`,
  `withStreetsAt`, `plusStreet`, `canBeSaved`), which is where the editing rules are unit-tested.
- **Coverage is a chip per city** — `Toute la ville` / `N rues`. Tapping it opens
  `CityStreetsBottomSheet`: an explicit "Toute la ville" / "Certaines rues" choice, removable
  chips, one-street-at-a-time entry with suggestions from the address API, and free text still
  accepted for a street the database does not know. Choosing "toute la ville" **clears** the list,
  because an empty list *is* how whole-commune coverage is expressed — letting the two disagree
  would save a path whose meaning differs from what it reads.
- **This is still the only UI that writes street restrictions**; a path must be re-saved here for
  its `city_entries` to exist in Firestore.
- Cities reorder with up/down arrows (deliberately not drag-and-drop: the order is the order the
  van drives and `locations` is aligned with it positionally). New city via
  `CityPostalCodeAutocompleteTextField`.
- `deliveryDay` is a single `FilterChip` selection over `DayOfWeek.entries` — **only one weekday per path** (matches `DeliveryPath.deliveryDay: String`, a single value not a set).
- Save is enabled only when name is non-blank, cities non-empty, and deliveryDay non-blank. The
  delete row appears **only when editing**, never while creating.
- Writes go `AdminViewModel.addNewDeliveryPath` / `updateDeliveryPath` / `deleteDeliveryPath` →
  `FirebaseAdminRepositoryImpl` → `FirestoreAdminDatasource` → `delivery_paths`
  (`admin/data/src/main/java/com/mtdevelopment/admin/data/source/FirestoreAdminDatasource.kt`).
- After a write the editor sets `PATH_LIST_NEEDS_REFRESH` on the **previous** back-stack entry and
  pops; the list screen sees it and calls `loadAdminData(forceRefresh = true)` once, bypassing the
  Room cache. The two screens own separate ViewModel instances (one per nav entry), which is why
  the flag exists at all — see Data caching below for why `forceRefresh` matters here.

**Keyboard layout gotcha, learned twice.** `imePadding()` must sit on the scrolled column and
**before** `verticalScroll` in the modifier chain: after it, it pads the content instead of
shrinking the viewport and the focused field stays under the keyboard. And it must not sit on the
`Box` that also holds the sticky action bar — that lifts the buttons with the IME and parks them
on top of the field being filled. In `CityStreetsBottomSheet` the street suggestions render
**above** the input for the same reason: a bottom sheet grows upward when the keyboard opens.

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

⚠️ **The column name is the Kotlin property name, not the `@SerialName`.** `PathEntity` carries
`@SerialName`s for kotlinx serialization, but no `@ColumnInfo`, so Room uses the property names —
which is what the `ALTER TABLE` statements in `FromagerieDatabase.kt` actually name.

| Column (Kotlin property) | Kotlin type | Notes |
|---|---|---|
| `id` | `String` | primary key |
| `name` | `String` | maps to `DeliveryPath.pathName` |
| `availableCities` | `Map<String, Int>` | city name → postcode (`@SerialName("cities")`) |
| `cityStreets` | `Map<String, List<String>>` | city name → its street allow-list; a city **absent from this map is served in full**. Added in schema v6 (`MIGRATION_5_6`, PR #59) |
| `locations` | `List<Coordinate>` | geocoded lat/lng centers, **aligned positionally** with the city order |
| `deliveryDay` | `String` | |
| `deliveryFrequency` | `String` | added in schema v5 (`MIGRATION_4_5`) |
| `geojson` | `String` | the whole `GeoJsonFeatureCollection` JSON-encoded as one text blob |

`toPath()` rejoins the two maps into the domain `List<DeliveryCity>`; `toPathEntity()` splits
them back, writing only the cities that actually have restrictions.

**Correction to the previous entry here (2026-07-29):** this skill used to warn that city
ordering "is not guaranteed to round-trip identically through a Map". In practice it does — the
converters go through `Json.encodeToString`/`decodeFromString`, both sides are `LinkedHashMap`,
and JSON preserves member order, so the sequence set in `PathEditDialog` survives. Verified by
`PathEntityTest` (`delivery/data`), which asserts city order and postcodes across the round trip.

The genuine hazard in the Map shape is different and narrower: **two entries with the same city
name inside one path collapse into one**, in both maps. That matters now that a split city is a
supported configuration — but a city is split across *two paths*, never twice within one, so it
does not bite the intended setup. Keep it in mind before "helpfully" allowing duplicate cities on
a single path.

## The `streets` city-split feature (was broken; FIXED 2026-07-29 PR #59, completed PR #60/#61)

**Goal** (owner-stated): let two paths cover the same city and split it by streets. Real example
— Path A (Tuesday): Boujailles, Frasne, Courvière. Path B (Friday): Boujailles,
Arc-sous-Montenot, Villers. Only Boujailles is split into two admin-defined street groups; the
customer's street decides which path's slot they get. Every other city stays unrestricted on its
own path.

**This now works.** How it works today is documented in the sections above — the shape in
"The `delivery_paths` Firestore document", the rules in "Matching an address to a path", the
admin entry point in "Path editing", the cache in "Room schema". **Read those, not this
section**, which is kept only so the defect pattern is not re-created.

PR #59 made the data shape right. Two more defects had to go before the feature was usable:

| # | Defect | Fixed in |
|---|---|---|
| 5 | The typed-address flow set the legacy booleans by hand instead of calling `updateEligibility`, so `streetNotCovered` was never raised there — on the commonest flow the customer saw **no button at all** | PR #60 |
| 6 | Two paths covering a commune unrestricted resolved by `firstOrNull()` on Firestore iteration order — arbitrary, and different from one session to the next | PR #60 |

Defect 5 is the instructive one: the rule existed, was unit-tested, and was correct — and was
invisible to real customers because a Composable posted the verdict into the wrong state fields.
**Domain tests passing is not evidence the UI shows the verdict.**

It was inert for two months behind four independent defects, each of which alone made it do
nothing visible — a useful reminder that "the feature does nothing" can be several bugs stacked,
and fixing one changes no observable behaviour:

| # | Defect | Now |
|---|---|---|
| 1 | Streets never written: the admin write DTO had no street field, so anything typed in the path dialog was dropped on save | `city_entries` carries them |
| 2 | Streets never survived Room: `PathEntity` had no column, and Room is the normal read path | `cityStreets` column + `MIGRATION_5_6` |
| 3 | Restrictions applied **path-wide** instead of per city, so a path restricting Boujailles made Frasne and Courvière undeliverable | matching is per (path, city) |
| 4 | `streets` was a flat list on the path — "Boujailles restricted, Frasne not" was unrepresentable | `DeliveryCity(name, postcode, streets)` |

Defect 3 was a regression from `657b721` ("Let Gemini 3.5 refact a whole lot of code"), which
replaced a direct `matchingPathForCity = path` with a three-step street gate whose last step
only accepted paths with an *empty* street list. Verified by replaying that matcher over the
real shop configuration: a Frasne address matched no path. See `fromagerie-failure-archaeology`
§12 — that refactor is no longer safe to assume purely mechanical.

**Why nothing caught it, and what to keep:** the matcher was a private function inside a
Composable file, duplicated between the typed-address and GPS flows, and no test in the repo
referenced `DeliveryEligibility`. It now lives in `delivery/domain` behind
`DetermineDeliveryEligibilityUseCase` with tests encoding the configuration above. **If you add
a third entry point, call the use case — do not copy the loop.** "No test names this symbol" is
itself a finding.

⚠️ **Operational note that outlives the fix:** a path only gains `city_entries` when the admin
**re-saves it** (now via `PathEditScreen`). A path that has never been re-saved since PR #59 still reads through the legacy
parallel arrays, i.e. every one of its cities is served in full. That is correct behaviour, not a
bug — but it is the first thing to check if a configured street split appears to be ignored.

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

All facts verified 2026-07-06 against the working tree (branch `claude/distracted-chaum-0986e4`),
**except the path/city/street model, the eligibility matcher and the `PathEntity` schema
(re-verified 2026-07-29, PR #59) and the resolution table, date building, address APIs and the
whole "Path editing" section (re-verified 2026-07-30, PR #60/#61).** Re-verify drift-prone claims:

- Firestore field names for `delivery_paths`: `grep -n "path_name\|delivery_day\|city_entries\|postcodes" delivery/data/src/main/java/com/mtdevelopment/delivery/data/source/remote/FirestoreDeliveryDataSource.kt`
- Streets still scoped per (path, city), not per path: `grep -n "streets" core/domain/src/main/java/com/mtdevelopment/core/model/DeliveryCity.kt` (a `streets` field reappearing on `DeliveryPath` itself is a regression)
- Eligibility matcher still single-sourced: `grep -rln "DetermineDeliveryEligibilityUseCase" delivery/presentation/src --include='*.kt'` (quote the glob — zsh expands it otherwise; expect exactly `CustomerContent.kt` and `PermissionManagerComposable.kt`, and a third copy of the matching loop is the bug PR #59 removed)
- Matching rules still hold: `./gradlew :delivery:domain:testDebugUnitTest` (`DetermineDeliveryEligibilityUseCaseTest` — one test per row of the resolution table — and `BuildSelectableDeliveryDatesUseCaseTest`)
- Ties still surface as a choice rather than a silent first-wins: `grep -n "candidatePaths" delivery/domain/src/main/java/com/mtdevelopment/delivery/domain/usecase/DetermineDeliveryEligibilityUseCase.kt` (a bare `firstOrNull()` deciding the winner is the regression)
- Path editor still a screen, dialog still gone: `ls delivery/presentation/src/admin/java/com/mtdevelopment/delivery/presentation/screen/PathEditScreen.kt` and `git log --oneline -1 -- delivery/presentation/src/admin/java/com/mtdevelopment/delivery/presentation/composable/PathEditDialog.kt`
- Path-editor rules still tested: `./gradlew :delivery:presentation:testAdminDebugUnitTest` (`PathDraftTest` — note the **admin** flavor task; `testClientDebugUnitTest` does not compile `src/admin`)
- Street suggestions still send the commune in the query: `grep -n "encodedPath" delivery/data/src/main/java/com/mtdevelopment/delivery/data/source/remote/AddressApiDataSource.kt` (a `type=street` call without the city in `q` returns a neighbouring commune's streets)
- Geocoding host regressed off the Géoplateforme: `grep -rn "api-adresse" --include='*.kt' . | grep -v /build/`
- `PathEntity` columns and Room round-trip: `grep -n "val [a-zA-Z]*:" delivery/data/src/main/java/com/mtdevelopment/delivery/data/model/entity/PathEntity.kt` and `./gradlew :delivery:data:testClientDebugUnitTest` (`PathEntityTest`)
- `database_update` document ids/fields: `grep -n "products_timestamp\|path_timestamp\|last_update" home/data/src/main/java/com/mtdevelopment/home/data/source/remote/FirestoreDatabase.kt`
- Admin still writes `database_update` timestamps: `grep -n "path_timestamp\|products_timestamp" admin/data/src/main/java/com/mtdevelopment/admin/data/source/FirestoreAdminDatasource.kt` (expect 2 hits — the `.set(` writers for both documents)
- Google Routes vs OpenRouteService split still holds: `grep -rn "GOOGLE_ROUTE_BASE_URL\|OPEN_ROUTE_BASE_URL" core/data/src/main/java/com/mtdevelopment/core/data/Constants.kt`
- MapBox public vs secret token distinction: `grep -n "MAPBOX_PUBLIC_TOKEN" delivery/presentation/src/client/java/com/mtdevelopment/delivery/presentation/screen/DeliveryOptionScreen.kt delivery/presentation/src/admin/java/com/mtdevelopment/delivery/presentation/screen/DeliveryOptionScreen.kt` and `grep -n "MAPBOX_SECRET_TOKEN" settings.gradle.kts`
- The 2026-07-30 dead-code deletions have not crept back: `grep -rn --include='*.kt' "ShippingSelectableDates\|DeliveryPathPickerComposable\|dateFieldText\|shouldDatePickerBeClickable" . | grep -v build` (expect zero hits; a hit on `ShippingSelectableDates` means the wrong 2-day lead time is back in the tree)
- Stop-tracking still manual (not automatic on foreground): `grep -n "isInTrackingMode\|ON_RESUME\|ON_STOP" admin/presentation/src/main/java/com/mtdevelopment/admin/presentation/screen/DeliveryHelperScreen.kt app/src/admin/java/com/mtdevelopment/lafromagerie/MainActivity.kt`
- Foreground-service fragility branch still present: `git branch -a | grep admin_delivery_instability`
- Preparation-status composite id format: `grep -n "statusId =" admin/presentation/src/main/java/com/mtdevelopment/admin/presentation/screen/OrderPreparationScreen.kt`
- Autocomplete hardcoded department restriction: `grep -n "terr=" core/data/src/main/java/com/mtdevelopment/core/source/AutoCompleteApiDataSource.kt`
