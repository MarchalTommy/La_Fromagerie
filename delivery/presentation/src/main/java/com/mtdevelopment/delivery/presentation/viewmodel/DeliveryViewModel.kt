package com.mtdevelopment.delivery.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtdevelopment.core.model.UserInformation
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import com.mtdevelopment.core.usecase.SaveToDatastoreUseCase
import com.mtdevelopment.delivery.domain.model.AutoCompleteSuggestion
import com.mtdevelopment.delivery.domain.usecase.GetAllDeliveryPathsUseCase
import com.mtdevelopment.delivery.domain.usecase.GetAutocompleteSuggestionsUseCase
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
                        addressSuggestions = emptyList()
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
                addressSearchQuery = userInfo?.address ?: "",
                userNameFieldText = userInfo?.name ?: "",
                selectedPath = deliveryUiDataState.deliveryPaths.firstOrNull { it.name == userInfo?.lastSelectedPath }
            )
            deliveryUiDataState = deliveryUiDataState.copy(isLoading = false)
        }

        ///////////////////////////////////////////////////////////////////////////
        // Autocomplete data
        ///////////////////////////////////////////////////////////////////////////
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .filter { it.length >= 5 }
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
                        addressSuggestions = emptyList()
                    )
                } else {
                    deliveryUiDataState.copy(showAddressSuggestions = true)
                }
            }
        }
    }

    fun saveUserInfo(onError: () -> Unit = {}) {
        viewModelScope.launch {
            if (deliveryUiDataState.selectedPath == null || deliveryUiDataState.addressSearchQuery.isBlank()) {
                onError.invoke()
                return@launch
            }
            saveToDatastoreUseCase.invoke(
                userInformation = UserInformation(
                    name = deliveryUiDataState.userNameFieldText,
                    address = deliveryUiDataState.addressSearchQuery,
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
                            "Si le problème persiste merci de nous contacter !"
                )
            })
    }

    private fun fetchAddressSuggestions(query: String) {
        setAddressesSuggestionsLoading(true)
        viewModelScope.launch {
            try {
                val suggestions = getAutocompleteSuggestionsUseCase.invoke(query)
                setAddressesSuggestions(suggestions.mapNotNull { it })
                setShowAddressesSuggestions(suggestions.isNotEmpty())
            } catch (e: Exception) {
                setShowAddressesSuggestions(false)
                setAddressesSuggestions(emptyList())
                // Afficher un message à l'utilisateur
                println("Erreur lors de la récupération des suggestions: ${e.message}")
            } finally {
                setAddressesSuggestionsLoading(false)
            }
        }
    }

    fun onSuggestionSelected(suggestion: AutoCompleteSuggestion) {
        suggestion.fulltext?.let { setAddressQuerySelected(it) }

        if (suggestion.lat != null && suggestion.lat != 0.0) {
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

    fun setAddressFieldText(address: String) {
        _searchQuery.value = address
        deliveryUiDataState = deliveryUiDataState.copy(addressSearchQuery = address)
    }

    fun setIsLoading(isLoading: Boolean) {
        deliveryUiDataState = deliveryUiDataState.copy(isLoading = isLoading)
    }

    fun setAddressQuerySelected(query: String) {
        deliveryUiDataState = deliveryUiDataState.copy(addressSearchQuery = query)
        _searchQuery.value = ""
        setAddressesSuggestions(emptyList())
        setShowAddressesSuggestions(false)
        updateLocalisationState(false)
    }

    fun setAddressesSuggestions(suggestions: List<AutoCompleteSuggestion>) {
        deliveryUiDataState = deliveryUiDataState.copy(addressSuggestions = suggestions)
    }

    fun setAddressesSuggestionsLoading(isLoading: Boolean) {
        deliveryUiDataState = deliveryUiDataState.copy(addressSuggestionsLoading = isLoading)
    }

    fun setShowAddressesSuggestions(shouldShow: Boolean) {
        deliveryUiDataState = deliveryUiDataState.copy(showAddressSuggestions = shouldShow)
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