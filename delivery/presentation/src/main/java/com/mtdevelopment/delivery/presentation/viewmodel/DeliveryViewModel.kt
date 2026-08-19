package com.mtdevelopment.delivery.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtdevelopment.core.model.AutoCompleteSuggestion
import com.mtdevelopment.core.model.DeliveryCity
import com.mtdevelopment.core.model.FulfillmentType
import com.mtdevelopment.core.model.PickupPointType
import com.mtdevelopment.core.model.UserInformation
import com.mtdevelopment.core.repository.SharedDatastore
import com.mtdevelopment.delivery.domain.usecase.BuildSelectablePickupDatesUseCase
import com.mtdevelopment.delivery.domain.usecase.GetPickupPointsUseCase
import com.mtdevelopment.delivery.domain.usecase.SelectablePickupDate
import java.time.LocalDateTime
import com.mtdevelopment.core.usecase.GetAutocompleteSuggestionsUseCase
import com.mtdevelopment.core.usecase.GetShopPickupSavingUseCase
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import com.mtdevelopment.core.usecase.SaveToDatastoreUseCase
import com.mtdevelopment.delivery.domain.usecase.CityResolution
import com.mtdevelopment.delivery.domain.usecase.DeliveryEligibility
import com.mtdevelopment.delivery.domain.usecase.GetAllDeliveryPathsUseCase
import com.mtdevelopment.delivery.domain.usecase.ResolveDeliveryCitiesUseCase
import com.mtdevelopment.delivery.domain.usecase.GetDeliveryPathUseCase
import com.mtdevelopment.delivery.domain.usecase.GetStreetSuggestionsUseCase
import com.mtdevelopment.delivery.domain.usecase.GetUserInfoFromDatastoreUseCase
import com.mtdevelopment.delivery.presentation.model.UiDeliveryPath
import com.mtdevelopment.delivery.presentation.model.toUiDeliveryPath
import com.mtdevelopment.delivery.presentation.state.DeliveryUiDataState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

/** Debounce before firing a street lookup, matching the address autocomplete's own delay. */
private const val STREET_SEARCH_DEBOUNCE_MS = 300L

@OptIn(FlowPreview::class)
/**
 * ViewModel for the Delivery module, handling logic for both the administrator (managing paths)
 * and the customer (choosing delivery options).
 * 
 * Key responsibilities:
 * 1. Admin Mode: Loading all delivery paths with GeoJSON for map visualization.
 * 2. Client Mode: Loading user info, matching the user's address to a delivery path.
 * 3. Address Autocomplete: Providing debounced suggestions for both delivery and billing addresses.
 * 4. State Management: Handling the multi-step delivery selection process (Name -> Address -> Path -> Date).
 */
class DeliveryViewModel(
    getIsConnectedUseCase: GetIsNetworkConnectedUseCase,
    private val getUserInfoFromDatastoreUseCase: GetUserInfoFromDatastoreUseCase,
    private val saveToDatastoreUseCase: SaveToDatastoreUseCase,
    private val getDeliveryPathsUseCase: GetDeliveryPathUseCase,
    private val getAllDeliveryPathsUseCase: GetAllDeliveryPathsUseCase,
    private val getAutocompleteSuggestionsUseCase: GetAutocompleteSuggestionsUseCase,
    private val getStreetSuggestionsUseCase: GetStreetSuggestionsUseCase,
    private val resolveDeliveryCitiesUseCase: ResolveDeliveryCitiesUseCase,
    private val getPickupPointsUseCase: GetPickupPointsUseCase,
    private val buildSelectablePickupDatesUseCase: BuildSelectablePickupDatesUseCase,
    private val getShopPickupSavingUseCase: GetShopPickupSavingUseCase,
    private val sharedDatastore: SharedDatastore
) : ViewModel(), KoinComponent {

    /**
     * Flow observing network connectivity.
     */
    val isConnected: StateFlow<Boolean> = getIsConnectedUseCase.invoke().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    /**
     * The primary UI state for the delivery module.
     */
    var deliveryUiDataState by mutableStateOf(DeliveryUiDataState())
        private set

    /**
     * Internal state for debouncing address search queries.
     */
    private val _deliverySearchQuery = MutableStateFlow("")
    private val _billingSearchQuery = MutableStateFlow("")

    private var deliveryAutocompleteJob: kotlinx.coroutines.Job? = null
    private var billingAutocompleteJob: kotlinx.coroutines.Job? = null
    private var reconnectRetryJob: kotlinx.coroutines.Job? = null
    private var shopPickupSavingJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            deliveryUiDataState = deliveryUiDataState.copy(isLoading = true)
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // ADMIN MODE LOGIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Loads data specific to the administrative view.
     * Fetches all delivery paths and their associated GeoJSON for map rendering.
     */
    fun loadAdminData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            getAllDeliveryPaths(forceRefresh = forceRefresh, withGeoJson = true)
        }

        // Setup debounced search for admin-side address autocomplete
        viewModelScope.launch {
            _deliverySearchQuery
                .debounce(300)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .filter { it.length >= 2 }
                .collectLatest { query ->
                    fetchAddressSuggestions(query)
                }
        }

        // Manage visibility of the suggestions dropdown
        viewModelScope.launch {
            _deliverySearchQuery.collect { query ->
                deliveryUiDataState = if (query.isBlank()) {
                    deliveryUiDataState.copy(
                        showAddressSuggestions = false,
                        deliveryAddressSuggestions = emptyList()
                    )
                } else {
                    deliveryUiDataState.copy(showAddressSuggestions = true)
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // CLIENT MODE LOGIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Loads data specific to the customer view.
     * Fetches delivery paths and populates UI with previously saved user info.
     */
    fun loadClientData() {
        viewModelScope.launch {
            getAllDeliveryPaths()
        }

        // Collected for as long as the screen lives rather than read once: the customer can
        // still change the basket from here, and a saving quoted against the basket they used
        // to have is worse than none. Job-guarded like reconnectRetryJob above -- a rotation
        // re-runs loadClientData(), and stacked collectors on one viewModelScope never stop.
        if (shopPickupSavingJob == null) {
            shopPickupSavingJob = viewModelScope.launch {
                getShopPickupSavingUseCase.invoke().collect { saving ->
                    deliveryUiDataState = deliveryUiDataState.copy(
                        shopPickupSavingInCents = saving
                    )
                }
            }
        }

        // Recovery from a failed first load. A launch with no network leaves the customer
        // with zero paths, and simply re-entering the screen does not help: the cache flag
        // decides whether to hit the network, not the screen lifecycle. Re-fetch, bypassing
        // the flag, as soon as connectivity is back AND we still have nothing to show.
        // Guarded by a job handle (same pattern as the autocomplete jobs) so repeated
        // loadClientData() calls do not stack collectors.
        if (reconnectRetryJob == null) {
            reconnectRetryJob = viewModelScope.launch {
                isConnected.collect { connected ->
                    if (connected && deliveryUiDataState.deliveryPaths.isEmpty()) {
                        getAllDeliveryPaths(forceRefresh = true)
                    }
                }
            }
        }

        // Populate fields with saved user info from DataStore
        viewModelScope.launch {
            val userInfo = getUserInfoFromDatastoreUseCase.invoke().firstOrNull()
            deliveryUiDataState = deliveryUiDataState.copy(
                userNameFieldText = userInfo?.name ?: "",
                deliveryAddressSearchQuery = userInfo?.address ?: "",
                // Match the saved path name to a full UI path object
                selectedPath = deliveryUiDataState.deliveryPaths.firstOrNull { it.name == userInfo?.lastSelectedPath },
            )
        }

        // Setup debounced search visibility for client-side autocomplete
        viewModelScope.launch {
            _deliverySearchQuery.collect { query ->
                deliveryUiDataState = if (query.isBlank()) {
                    deliveryUiDataState.copy(
                        showAddressSuggestions = false,
                        deliveryAddressSuggestions = emptyList()
                    )
                } else {
                    deliveryUiDataState.copy(showAddressSuggestions = true)
                }
            }
        }
        viewModelScope.launch {
            _billingSearchQuery.collect { query ->
                deliveryUiDataState = if (query.isBlank()) {
                    deliveryUiDataState.copy(
                        showBillingAddressSuggestions = false,
                        billingAddressSuggestions = emptyList()
                    )
                } else {
                    deliveryUiDataState.copy(showBillingAddressSuggestions = true)
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Pickup (click & collect and markets)
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Switches between being delivered and collecting, and loads what the new mode needs.
     *
     * Nothing already typed is cleared: a customer who tries "retrait" and comes back to
     * "livraison" would otherwise have to retype their address.
     */
    fun setFulfillmentType(type: FulfillmentType) {
        // The chosen date belongs to the mode it was picked in. Carrying it across would let
        // the customer confirm a shop date while the state says market.
        deliveryUiDataState = deliveryUiDataState.copy(
            fulfillmentType = type,
            selectedPickupDate = null
        )
        // Persisted globally, not kept on this screen: the catalogue prices itself from it,
        // so the customer sees what they will be charged while filling their basket rather
        // than discovering it at payment.
        viewModelScope.launch { sharedDatastore.setFulfillmentType(type.name) }
        if (type.isPickup) loadPickupPoints(type)
    }

    fun setUserPhoneFieldText(phone: String) {
        deliveryUiDataState = deliveryUiDataState.copy(userPhoneFieldText = phone)
    }

    /** Records which collection date the customer tapped, before they confirm it. */
    fun setSelectedPickupDate(selection: SelectablePickupDate?) {
        deliveryUiDataState = deliveryUiDataState.copy(selectedPickupDate = selection)
    }

    /**
     * Loads the points matching [type] and the dates they offer.
     *
     * A read failure raises [DeliveryUiDataState.pickupPointsUnavailable] rather than leaving
     * an empty list behind: "we could not check" and "you cannot collect" look identical on
     * screen and mean opposite things, and only one of them should cost the shop a sale.
     *
     * Both callbacks drop their answer when the customer has switched mode since the read
     * started. Switching Boutique → Marché quickly leaves two reads in flight and the last
     * one to *arrive* wins, which is how the state ends up claiming PICKUP_MARKET while
     * offering the shop's dates — and an order snapshotted onto the wrong pickup point.
     */
    fun loadPickupPoints(type: FulfillmentType = deliveryUiDataState.fulfillmentType) {
        val wanted = when (type) {
            FulfillmentType.PICKUP_SHOP -> PickupPointType.SHOP
            FulfillmentType.PICKUP_MARKET -> PickupPointType.MARKET
            FulfillmentType.DELIVERY -> return
        }

        deliveryUiDataState = deliveryUiDataState.copy(
            isLoading = true,
            pickupPointsUnavailable = false
        )
        getPickupPointsUseCase.invoke(
            onSuccess = { points ->
                if (deliveryUiDataState.fulfillmentType != type) return@invoke
                val matching = points.filter { it.type == wanted }
                deliveryUiDataState = deliveryUiDataState.copy(
                    isLoading = false,
                    pickupPoints = matching,
                    pickupDates = buildSelectablePickupDatesUseCase.invoke(
                        points = matching,
                        now = LocalDateTime.now()
                    )
                )
            },
            onFailure = {
                if (deliveryUiDataState.fulfillmentType != type) return@invoke
                deliveryUiDataState = deliveryUiDataState.copy(
                    isLoading = false,
                    pickupPoints = emptyList(),
                    pickupDates = emptyList(),
                    pickupPointsUnavailable = true
                )
            }
        )
    }

    /**
     * Persists the chosen collection date and the point it belongs to.
     *
     * The point's label, address and opening window are stored alongside the id so the order
     * carries a snapshot: a market date edited or deleted afterwards must not rewrite a
     * purchase already made.
     *
     * Everything the customer is not choosing here survives untouched, the home address
     * included: the datastore holds who the customer is, not what this order is. The two were
     * the same object while delivery was the only mode, and separating them is what lets
     * [setFulfillmentType] keep its promise that trying collection clears nothing.
     */
    fun savePickupSelection(selection: SelectablePickupDate) {
        viewModelScope.launch {
            val existingUserInfo = getUserInfoFromDatastoreUseCase.invoke().firstOrNull()
            saveToDatastoreUseCase.invoke(
                userInformation = UserInformation(
                    name = deliveryUiDataState.userNameFieldText,
                    email = existingUserInfo?.email ?: "",
                    // The remembered home address, kept. It is not this order's delivery
                    // address — a collected order carries none, and CheckoutViewModel blanks
                    // it there. Clearing it here instead would cost a returning customer the
                    // address they typed once, permanently, for having tried collection.
                    address = existingUserInfo?.address ?: "",
                    billingAddress = existingUserInfo?.billingAddress ?: "",
                    lastSelectedPath = existingUserInfo?.lastSelectedPath ?: "",
                    phone = deliveryUiDataState.userPhoneFieldText,
                    fulfillmentType = deliveryUiDataState.fulfillmentType.name,
                    pickupPointId = selection.pointId,
                    pickupLabel = selection.pointLabel,
                    pickupAddress = selection.pointAddress,
                    pickupTimeRange = selection.timeRange
                ),
                deliveryDate = selection.date
                    .atStartOfDay(java.time.ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            )
        }
    }

    /**
     * Persists the user's name, address, and selected path to the DataStore.
     */
    fun saveUserInfo(onError: () -> Unit = {}) {
        viewModelScope.launch {
            // Validation: Ensure mandatory fields are filled.
            // A null selectedPath is not a failure when several tournées are still in the running:
            // the customer settles it by picking a date, and this runs again with the winner.
            if (deliveryUiDataState.deliveryAddressSearchQuery.isBlank() ||
                deliveryUiDataState.userNameFieldText.isBlank() ||
                (deliveryUiDataState.selectedPath == null &&
                        deliveryUiDataState.candidatePaths.isEmpty())
            ) {
                onError.invoke()
                return@launch
            }
            val existingUserInfo = getUserInfoFromDatastoreUseCase.invoke().firstOrNull()
            saveToDatastoreUseCase.invoke(
                userInformation = UserInformation(
                    name = deliveryUiDataState.userNameFieldText,
                    email = existingUserInfo?.email ?: "",
                    address = deliveryUiDataState.deliveryAddressSearchQuery,
                    billingAddress = deliveryUiDataState.billingAddressSearchQuery,
                    lastSelectedPath = deliveryUiDataState.selectedPath?.name
                        ?: existingUserInfo?.lastSelectedPath ?: "",
                    phone = deliveryUiDataState.userPhoneFieldText.ifBlank { null },
                    // Explicitly reset: a customer who tried collecting and went back to
                    // being delivered must not carry a stale pickup point into checkout.
                    fulfillmentType = FulfillmentType.DELIVERY.name,
                    pickupPointId = null,
                    pickupLabel = null,
                    pickupAddress = null,
                    pickupTimeRange = null
                )
            )
        }
    }

    /**
     * Persists the selected delivery date.
     */
    fun saveSelectedDate(date: Long) {
        viewModelScope.launch {
            saveToDatastoreUseCase.invoke(deliveryDate = date)
        }
    }

    /**
     * Internal method to fetch delivery paths from the repository.
     * @param withGeoJson If true, fetches geographic coordinates for path visualization.
     */
    private suspend fun getAllDeliveryPaths(
        forceRefresh: Boolean = false,
        withGeoJson: Boolean = false
    ) {
        getAllDeliveryPathsUseCase.invoke(
            forceRefresh = forceRefresh,
            withGeoJson = withGeoJson,
            scope = viewModelScope,
            onSuccess = { pathsList ->
                deliveryUiDataState = deliveryUiDataState.copy(
                    deliveryPaths = pathsList.mapNotNull { path ->
                        path?.toUiDeliveryPath()
                    },
                    isLoading = false
                )
            },
            onFailure = {
                deliveryUiDataState = deliveryUiDataState.copy(
                    isLoading = false,
                    isError = "Une erreur est survenue lors du chargement des parcours de livraison.\n" +
                            "Si le problème persiste merci de nous contacter !",
                )
            })
    }

    /**
     * Starts observing the autocomplete search query.
     * @param isBilling If true, targets the billing address field instead of delivery.
     */
    fun startAutocomplete(isBilling: Boolean = false) {
        if (isBilling) {
            if (billingAutocompleteJob == null) {
                billingAutocompleteJob = viewModelScope.launch {
                    _billingSearchQuery
                        .debounce(300)
                        .distinctUntilChanged()
                        .filter { it.isNotBlank() }
                        .filter { it.length >= 5 }
                        .collectLatest { query ->
                            fetchAddressSuggestions(query, isBilling = true)
                        }
                }
            }
        } else {
            if (deliveryAutocompleteJob == null) {
                deliveryAutocompleteJob = viewModelScope.launch {
                    _deliverySearchQuery
                        .debounce(300)
                        .distinctUntilChanged()
                        .filter { it.isNotBlank() }
                        .filter { it.length >= 5 }
                        .collectLatest { query ->
                            fetchAddressSuggestions(query, isBilling = false)
                        }
                }
            }
        }
    }

    /**
     * Fetches address suggestions from the external API.
     */
    private fun fetchAddressSuggestions(query: String, isBilling: Boolean = false) {
        setAddressesSuggestionsLoading(true)
        viewModelScope.launch {
            try {
                val suggestions = getAutocompleteSuggestionsUseCase.invoke(query)
                setAddressesSuggestions(suggestions.mapNotNull { it }, isBilling = isBilling)
                setShowAddressesSuggestions(suggestions.isNotEmpty(), isBilling = isBilling)
            } catch (e: Exception) {
                setShowAddressesSuggestions(false, isBilling = isBilling)
                setAddressesSuggestions(emptyList(), isBilling = isBilling)
                println("Erreur lors de la récupération des suggestions: ${e.message}")
            } finally {
                setAddressesSuggestionsLoading(false)
            }
        }
    }

    /**
     * Handles selection of a suggestion from the autocomplete list.
     * Updates the text field and triggers location update for the selected city.
     */
    fun onSuggestionSelected(suggestion: AutoCompleteSuggestion, isBilling: Boolean = false) {
        suggestion.fulltext?.let { setAddressQuerySelected(it, isBilling) }

        // If it's a delivery address, update the map center and check for path proximity
        if (suggestion.lat != null && suggestion.lat != 0.0 && !isBilling) {
            updateUserCityLocation(Pair(suggestion.lat ?: 0.0, suggestion.long ?: 0.0))
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // UI STATE UPDATERS
    ///////////////////////////////////////////////////////////////////////////

    fun setIsDatePickerShown(isShown: Boolean) {
        deliveryUiDataState = deliveryUiDataState.copy(datePickerVisibility = isShown)
    }

    fun setUserNameFieldText(name: String) {
        deliveryUiDataState = deliveryUiDataState.copy(userNameFieldText = name)
    }

    fun setAddressFieldText(address: String, isBilling: Boolean = false) {
        deliveryUiDataState = if (isBilling) {
            deliveryUiDataState.copy(billingAddressSearchQuery = address)
        } else {
            deliveryUiDataState.copy(deliveryAddressSearchQuery = address)
        }
        if (isBilling) {
            _billingSearchQuery.value = address
        } else {
            _deliverySearchQuery.value = address
        }
    }

    fun setIsLoading(isLoading: Boolean) {
        deliveryUiDataState = deliveryUiDataState.copy(isLoading = isLoading)
    }

    /**
     * Finalizes the address selection.
     */
    fun setAddressQuerySelected(query: String, isBilling: Boolean = false) {
        deliveryUiDataState = if (isBilling) {
            deliveryUiDataState.copy(billingAddressSearchQuery = query)
        } else {
            // Reset localisation success if a manual address is chosen
            updateLocalisationState(false)
            deliveryUiDataState.copy(deliveryAddressSearchQuery = query)
        }
        if (isBilling) {
            _billingSearchQuery.value = ""
        } else {
            _deliverySearchQuery.value = ""
        }
        setAddressesSuggestions(emptyList(), isBilling = isBilling)
        setShowAddressesSuggestions(false, isBilling = isBilling)
    }

    fun setAddressesSuggestions(
        suggestions: List<AutoCompleteSuggestion>,
        isBilling: Boolean = false
    ) {
        deliveryUiDataState = if (isBilling) {
            deliveryUiDataState.copy(
                billingAddressSuggestions = suggestions,
                deliveryAddressSuggestions = emptyList()
            )
        } else {
            deliveryUiDataState.copy(
                deliveryAddressSuggestions = suggestions,
                billingAddressSuggestions = emptyList()
            )
        }
    }

    fun setAddressesSuggestionsLoading(isLoading: Boolean) {
        deliveryUiDataState = deliveryUiDataState.copy(addressSuggestionsLoading = isLoading)
    }

    fun setShowAddressesSuggestions(shouldShow: Boolean, isBilling: Boolean) {
        deliveryUiDataState = if (isBilling) {
            deliveryUiDataState.copy(showBillingAddressSuggestions = shouldShow)
        } else {
            deliveryUiDataState.copy(showAddressSuggestions = shouldShow)
        }
    }

    fun setIsError(isError: String) {
        deliveryUiDataState = deliveryUiDataState.copy(isError = isError)
    }

    fun setColumnScrollingEnabled(isEnabled: Boolean) {
        deliveryUiDataState = deliveryUiDataState.copy(columnScrollingEnabled = isEnabled)
    }

    fun updateUserCityLocation(location: Pair<Double, Double>) {
        deliveryUiDataState = deliveryUiDataState.copy(userCityLocation = location)
    }

    fun updateUserCity(city: String) {
        deliveryUiDataState = deliveryUiDataState.copy(userCity = city)
    }

    fun updateSelectedPath(path: UiDeliveryPath?) {
        deliveryUiDataState = deliveryUiDataState.copy(selectedPath = path)
    }

    /**
     * Records every path that serves the customer's address, and settles on one when it can.
     *
     * A single candidate is selected outright. Several means the shop genuinely covers the address
     * on more than one tournée: we keep the customer's previous pick if it is still on the list —
     * so re-entering the screen does not silently move them — and otherwise leave [selectedPath]
     * null so the merged date picker is what assigns the path.
     */
    fun updateCandidatePaths(paths: List<UiDeliveryPath>) {
        val previousId = deliveryUiDataState.selectedPath?.id
        deliveryUiDataState = deliveryUiDataState.copy(
            candidatePaths = paths,
            selectedPath = when {
                paths.isEmpty() -> null
                paths.size == 1 -> paths.single()
                else -> paths.firstOrNull { it.id == previousId }
            }
        )
    }

    fun setIsBillingDifferent(isDifferent: Boolean) {
        deliveryUiDataState = deliveryUiDataState.copy(isBillingDifferent = isDifferent)
    }

    ///////////////////////////////////////////////////////////////////////////
    // PERMISSION & PROXIMITY STATE
    ///////////////////////////////////////////////////////////////////////////

    fun updateShouldShowLocalisationPermission(isGranted: Boolean) {
        deliveryUiDataState =
            deliveryUiDataState.copy(shouldShowLocalisationPermission = isGranted)
    }

    fun updateLocalisationState(isAcquired: Boolean) {
        deliveryUiDataState =
            deliveryUiDataState.copy(localisationSuccess = isAcquired)
    }

    fun updateUserLocationOnPath(isOnPath: Boolean) {
        deliveryUiDataState =
            deliveryUiDataState.copy(userLocationOnPath = isOnPath)
    }

    fun updateUserLocationCloseFromPath(isClose: Boolean) {
        deliveryUiDataState =
            deliveryUiDataState.copy(userLocationCloseFromPath = isClose)
    }

    /**
     * Applies a [DeliveryEligibility] verdict to the UI state.
     *
     * [DeliveryEligibility.STREET_NOT_COVERED] shares the "ask for support" affordance with
     * [DeliveryEligibility.ASK_FOR_SUPPORT] but carries its own wording, hence the extra flag.
     */
    ///////////////////////////////////////////////////////////////////////////
    // STREET SUGGESTIONS (admin path editor)
    ///////////////////////////////////////////////////////////////////////////

    private var streetSearchJob: Job? = null

    /**
     * Looks up street names of one commune for the path editor.
     *
     * Debounced and single-flight: the shop types a few characters at a time and each keystroke
     * would otherwise fire its own request, with the answers landing out of order.
     */
    fun searchStreets(query: String, city: String, postcode: Int) {
        streetSearchJob?.cancel()
        if (query.isBlank() || city.isBlank()) {
            deliveryUiDataState = deliveryUiDataState.copy(streetSuggestions = emptyList())
            return
        }
        streetSearchJob = viewModelScope.launch {
            delay(STREET_SEARCH_DEBOUNCE_MS)
            val suggestions = runCatching {
                getStreetSuggestionsUseCase.invoke(query, city, postcode)
            }.getOrDefault(emptyList())
            deliveryUiDataState = deliveryUiDataState.copy(streetSuggestions = suggestions)
        }
    }

    fun clearStreetSuggestions() {
        streetSearchJob?.cancel()
        deliveryUiDataState = deliveryUiDataState.copy(streetSuggestions = emptyList())
    }

    /**
     * Checks a path's cities against the address API and hands back the canonical spelling of each.
     *
     * Admin-only, but it lives here alongside [loadAdminData] and [searchStreets] rather than in a
     * ViewModel of its own: the path editor deliberately has none, and its city field already goes
     * through this one.
     *
     * Never throws and never reports a misspelling it is not sure about — see
     * [ResolveDeliveryCitiesUseCase]. On an outright failure the report comes back empty, which the
     * editor reads as "nothing to say".
     */
    fun resolveCities(
        cities: List<DeliveryCity>,
        onResolved: (List<CityResolution>) -> Unit
    ) {
        if (cities.isEmpty()) {
            onResolved(emptyList())
            return
        }
        viewModelScope.launch {
            onResolved(
                runCatching { resolveDeliveryCitiesUseCase.invoke(cities) }
                    .getOrDefault(emptyList())
            )
        }
    }

    fun updateEligibility(eligibility: DeliveryEligibility) {
        deliveryUiDataState = deliveryUiDataState.copy(
            userLocationOnPath = eligibility == DeliveryEligibility.DELIVERABLE,
            userLocationCloseFromPath = eligibility == DeliveryEligibility.ASK_FOR_SUPPORT ||
                    eligibility == DeliveryEligibility.STREET_NOT_COVERED,
            streetNotCovered = eligibility == DeliveryEligibility.STREET_NOT_COVERED
        )
    }
}