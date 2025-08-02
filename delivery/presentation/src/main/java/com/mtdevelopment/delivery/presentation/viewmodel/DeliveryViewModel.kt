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
class DeliveryViewModel(
    getIsConnectedUseCase: GetIsNetworkConnectedUseCase,
    private val getUserInfoFromDatastoreUseCase: GetUserInfoFromDatastoreUseCase,
    private val saveToDatastoreUseCase: SaveToDatastoreUseCase,
    private val getDeliveryPathsUseCase: GetDeliveryPathUseCase,
    private val getAllDeliveryPathsUseCase: GetAllDeliveryPathsUseCase,
    private val getAutocompleteSuggestionsUseCase: GetAutocompleteSuggestionsUseCase
) : ViewModel(), KoinComponent {

    val isConnected: StateFlow<Boolean> = getIsConnectedUseCase.invoke().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    var deliveryUiDataState by mutableStateOf(DeliveryUiDataState())
        private set

    // Private autocomplete query state
    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            deliveryUiDataState = deliveryUiDataState.copy(isLoading = true)
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // ADMIN
    ///////////////////////////////////////////////////////////////////////////
    fun loadAdminData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            getAllDeliveryPaths(forceRefresh = forceRefresh, withGeoJson = true)
        }

        ///////////////////////////////////////////////////////////////////////////
        // Autocomplete data
        ///////////////////////////////////////////////////////////////////////////
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .filter { it.length >= 2 }
                .collectLatest { query ->
                    fetchAddressSuggestions(query)
                }
        }

        // Cache le dropdown si la requête est vide
        viewModelScope.launch {
            _searchQuery.collect { query ->
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
    // CLIENT
    ///////////////////////////////////////////////////////////////////////////
    fun loadClientData() {
        viewModelScope.launch {
            getAllDeliveryPaths()
        }
        ///////////////////////////////////////////////////////////////////////////
        // User Data to populate UI
        ///////////////////////////////////////////////////////////////////////////
        viewModelScope.launch {
            val userInfo = getUserInfoFromDatastoreUseCase.invoke().firstOrNull()
            deliveryUiDataState = deliveryUiDataState.copy(
                userNameFieldText = userInfo?.name ?: "",
                deliveryAddressSearchQuery = userInfo?.address ?: "",
                selectedPath = deliveryUiDataState.deliveryPaths.firstOrNull { it.name == userInfo?.lastSelectedPath },
            )
            deliveryUiDataState = deliveryUiDataState.copy()
        }

        ///////////////////////////////////////////////////////////////////////////
        // Autocomplete data
        ///////////////////////////////////////////////////////////////////////////
        // Cache le dropdown si la requête est vide
        viewModelScope.launch {
            _searchQuery.collect { query ->
                deliveryUiDataState = if (query.isBlank()) {
                    deliveryUiDataState.copy(
                        showAddressSuggestions = false,
                        deliveryAddressSuggestions = emptyList(),
                        billingAddressSuggestions = emptyList()
                    )
                } else {
                    deliveryUiDataState.copy(showAddressSuggestions = true)
                }
            }
        }
    }

    fun saveUserInfo(onError: () -> Unit = {}) {
        viewModelScope.launch {
            if (deliveryUiDataState.selectedPath == null || deliveryUiDataState.deliveryAddressSearchQuery.isBlank()) {
                onError.invoke()
                return@launch
            }
            saveToDatastoreUseCase.invoke(
                userInformation = UserInformation(
                    name = deliveryUiDataState.userNameFieldText,
                    address = deliveryUiDataState.deliveryAddressSearchQuery,
                    billingAddress = deliveryUiDataState.billingAddressSearchQuery,
                    lastSelectedPath = deliveryUiDataState.selectedPath?.name ?: ""
                )
            )
        }
    }

    fun saveSelectedDate(date: Long) {
        viewModelScope.launch {
            saveToDatastoreUseCase.invoke(deliveryDate = date)
        }
    }

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

    fun startAutocomplete(isBilling: Boolean = false) {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .filter { it.length >= 5 }
                .collectLatest { query ->
                    fetchAddressSuggestions(query, isBilling)
                }
        }
    }

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
                // Afficher un message à l'utilisateur
                println("Erreur lors de la récupération des suggestions: ${e.message}")
            } finally {
                setAddressesSuggestionsLoading(false)
            }
        }
    }

    fun onSuggestionSelected(suggestion: AutoCompleteSuggestion, isBilling: Boolean = false) {
        suggestion.fulltext?.let { setAddressQuerySelected(it, isBilling) }

        if (suggestion.lat != null && suggestion.lat != 0.0 && !isBilling) {
            updateUserCityLocation(Pair(suggestion.lat ?: 0.0, suggestion.long ?: 0.0))
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // DELIVERY STATE
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
        _searchQuery.value = address
    }

    fun setIsLoading(isLoading: Boolean) {
        deliveryUiDataState = deliveryUiDataState.copy(isLoading = isLoading)
    }

    fun setAddressQuerySelected(query: String, isBilling: Boolean = false) {
        deliveryUiDataState = if (isBilling) {
            deliveryUiDataState.copy(billingAddressSearchQuery = query)
        } else {
            updateLocalisationState(false)
            deliveryUiDataState.copy(deliveryAddressSearchQuery = query)
        }
        _searchQuery.value = ""
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
    // PERMISSION MANAGER STATE
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