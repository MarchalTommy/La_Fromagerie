package com.mtdevelopment.delivery.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtdevelopment.core.model.AutoCompleteSuggestion
import com.mtdevelopment.core.model.UserInformation
import com.mtdevelopment.core.usecase.GetAutocompleteSuggestionsUseCase
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import com.mtdevelopment.core.usecase.SaveToDatastoreUseCase
import com.mtdevelopment.delivery.domain.usecase.GetAllDeliveryPathsUseCase
import com.mtdevelopment.delivery.domain.usecase.GetDeliveryPathUseCase
import com.mtdevelopment.delivery.domain.usecase.GetUserInfoFromDatastoreUseCase
import com.mtdevelopment.delivery.presentation.model.UiDeliveryPath
import com.mtdevelopment.delivery.presentation.model.toUiDeliveryPath
import com.mtdevelopment.delivery.presentation.state.DeliveryUiDataState
import kotlinx.coroutines.FlowPreview
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
    private val getAutocompleteSuggestionsUseCase: GetAutocompleteSuggestionsUseCase
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

    /**
     * Persists the user's name, address, and selected path to the DataStore.
     */
    fun saveUserInfo(onError: () -> Unit = {}) {
        viewModelScope.launch {
            // Validation: Ensure mandatory fields are filled
            if (deliveryUiDataState.selectedPath == null || 
                deliveryUiDataState.deliveryAddressSearchQuery.isBlank() ||
                deliveryUiDataState.userNameFieldText.isBlank()
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
                    lastSelectedPath = deliveryUiDataState.selectedPath?.name ?: ""
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

    fun setIsDatePickerClickable(isClickable: Boolean) {
        deliveryUiDataState =
            deliveryUiDataState.copy(shouldDatePickerBeClickable = isClickable)
    }

    fun setIsDatePickerShown(isShown: Boolean) {
        deliveryUiDataState = deliveryUiDataState.copy(datePickerVisibility = isShown)
    }

    fun setDateFieldText(text: String) {
        deliveryUiDataState = deliveryUiDataState.copy(dateFieldText = text)
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
}