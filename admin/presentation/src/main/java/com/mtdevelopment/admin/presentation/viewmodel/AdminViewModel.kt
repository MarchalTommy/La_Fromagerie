package com.mtdevelopment.admin.presentation.viewmodel

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtdevelopment.admin.domain.model.OptimizedRouteWithOrders
import com.mtdevelopment.admin.domain.usecase.AddNewPathUseCase
import com.mtdevelopment.admin.domain.usecase.AddNewProductUseCase
import com.mtdevelopment.admin.domain.usecase.DeletePathUseCase
import com.mtdevelopment.admin.domain.usecase.DeleteProductUseCase
import com.mtdevelopment.admin.domain.usecase.GetAllOrdersUseCase
import com.mtdevelopment.admin.domain.usecase.GetCurrentLocationOnceUseCase
import com.mtdevelopment.admin.domain.usecase.GetIsInTrackingModeUseCase
import com.mtdevelopment.admin.domain.usecase.GetOptimizedDeliveryUseCase
import com.mtdevelopment.admin.domain.usecase.GetShouldShowBatterieOptimizationUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateDeliveryPathUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateProductUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateShouldShowBatterieOptimizationUseCase
import com.mtdevelopment.admin.domain.usecase.UploadImageUseCase
import com.mtdevelopment.admin.presentation.model.OrderScreenState
import com.mtdevelopment.core.domain.toTimeStamp
import com.mtdevelopment.core.model.AutoCompleteSuggestion
import com.mtdevelopment.core.model.DeliveryPath
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.presentation.sharedModels.toDomainProduct
import com.mtdevelopment.core.usecase.GetAutocompleteSuggestionsUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import java.time.Instant

@OptIn(FlowPreview::class)
class AdminViewModel(
    private val updateProductUseCase: UpdateProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val addNewProductUseCase: AddNewProductUseCase,
    private val updateDeliveryPathUseCase: UpdateDeliveryPathUseCase,
    private val deleteDeliveryPathUseCase: DeletePathUseCase,
    private val addNewDeliveryPathUseCase: AddNewPathUseCase,
    private val getAllOrdersUseCase: GetAllOrdersUseCase,
    private val uploadImageUseCase: UploadImageUseCase,
    private val isInTrackingModeUseCase: GetIsInTrackingModeUseCase,
    private val getOptimizedDeliveryUseCase: GetOptimizedDeliveryUseCase,
    private val getCurrentLocationOnceUseCase: GetCurrentLocationOnceUseCase,
    private val getAutocompleteSuggestionsUseCase: GetAutocompleteSuggestionsUseCase,
    private val shouldShowBatterieOptimizationUseCase: UpdateShouldShowBatterieOptimizationUseCase,
    private val getShouldShowBatterieOptimizationUseCase: GetShouldShowBatterieOptimizationUseCase,
) : ViewModel(), KoinComponent {

    ///////////////////////////////////////////////////////////////////////////
    // States
    ///////////////////////////////////////////////////////////////////////////
    private val _orderScreenState = MutableStateFlow(OrderScreenState())
    val orderScreenState = _orderScreenState.asStateFlow()

    // Private autocomplete query state
    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            getShouldShowBatterieOptimizationUseCase.invoke().collect {
                _orderScreenState.value = _orderScreenState.value.copy(
                    shouldShowBatterieOptimization = it
                )
            }
        }
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
                _orderScreenState.value = if (query.isBlank()) {
                    orderScreenState.value.copy(
                        showSuggestions = false,
                        suggestions = emptyList()
                    )
                } else {
                    _orderScreenState.value.copy(showSuggestions = true)
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Autocomplete
    ///////////////////////////////////////////////////////////////////////////
    private fun fetchAddressSuggestions(query: String) {
        _orderScreenState.value = _orderScreenState.value.copy(
            suggestionsLoading = true
        )
        viewModelScope.launch {
            try {
                val suggestions = getAutocompleteSuggestionsUseCase.invoke(query)
                _orderScreenState.value = _orderScreenState.value.copy(
                    suggestions = suggestions.mapNotNull { it },
                    suggestionsLoading = false,
                    showSuggestions = suggestions.isNotEmpty()
                )
            } catch (e: Exception) {
                _orderScreenState.value = _orderScreenState.value.copy(
                    suggestions = emptyList(),
                    suggestionsLoading = false,
                    showSuggestions = false
                )
                // Afficher un message à l'utilisateur
                println("Erreur lors de la récupération des suggestions: ${e.message}")
            }
        }
    }

    fun addOrder(order: Order) {
        viewModelScope.launch {
            _orderScreenState.value = _orderScreenState.value.copy(
                orders = _orderScreenState.value.orders + order
            )
        }
    }

    fun onSuggestionSelected(suggestion: AutoCompleteSuggestion) {
        suggestion.fulltext?.let {
            _orderScreenState.value = _orderScreenState.value.copy(
                searchQuery = it
            )
        }
    }

    fun setShowAddressesSuggestions(shouldShow: Boolean) {
        _orderScreenState.value = _orderScreenState.value.copy(showSuggestions = shouldShow)
    }

    fun setAddressText(address: String) {
        _orderScreenState.value = _orderScreenState.value.copy(searchQuery = address)
        _searchQuery.value = address
    }

    fun setDialogVisibility(isVisible: Boolean) {
        _orderScreenState.value = _orderScreenState.value.copy(shouldShowDialog = isVisible)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Products
    ///////////////////////////////////////////////////////////////////////////
    fun updateProduct(
        product: UiProductObject,
        onLoading: (Boolean) -> Unit,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            onLoading.invoke(true)
            product.imageUrl?.toUri()?.let {
                uploadImageUseCase.invoke(
                    imageUri = it,
                    onResult = { result ->
                        result.onSuccess { onlineUrl ->
                            product.imageUrl = onlineUrl
                        }
                    }
                )
            }

            updateProductUseCase.invoke(product.toDomainProduct(), onSuccess = {
                onSuccess.invoke()
            }, onError = {
                onError.invoke()
            })
            onLoading.invoke(false)
        }
    }

    fun deleteProduct(product: UiProductObject) {
        viewModelScope.launch {
            deleteProductUseCase.invoke(product.toDomainProduct(), onSuccess = {

            }, onError = {

            })
        }
    }

    fun addNewProduct(
        product: UiProductObject,
        onLoading: (Boolean) -> Unit,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            onLoading.invoke(true)
            product.imageUrl?.toUri()?.let {
                uploadImageUseCase.invoke(
                    imageUri = it,
                    onResult = { result ->
                        result.onSuccess { onlineUrl ->
                            product.imageUrl = onlineUrl
                        }
                    }
                )
            }

            addNewProductUseCase.invoke(product.toDomainProduct(), onSuccess = {
                onSuccess.invoke()
            }, onError = {
                onError.invoke()
            })
            onLoading.invoke(false)
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Delivery Paths
    ///////////////////////////////////////////////////////////////////////////
    fun updateDeliveryPath(path: DeliveryPath, onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            updateDeliveryPathUseCase.invoke(path, onSuccess = {
                onSuccess.invoke()
            }, onError = {
                onFailure.invoke()
            })
        }
    }

    fun deleteDeliveryPath(path: DeliveryPath, onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            deleteDeliveryPathUseCase.invoke(path, onSuccess = {
                onSuccess.invoke()
            }, onError = {
                onFailure.invoke()
            })
        }
    }


    fun addNewDeliveryPath(path: DeliveryPath, onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            addNewDeliveryPathUseCase.invoke(path, onSuccess = {
                onSuccess.invoke()
            }, onError = {
                onFailure.invoke()
            })
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Orders
    ///////////////////////////////////////////////////////////////////////////
    fun getAllOrders() {
        viewModelScope.launch {
            _orderScreenState.value = _orderScreenState.value.copy(
                isLoading = true
            )

        }
        viewModelScope.launch {
            getAllOrdersUseCase.invoke(onSuccess = {
                _orderScreenState.value = _orderScreenState.value.copy(
                    orders = it ?: emptyList(),
                    error = if (it == null) "Error fetching orders" else null,
                    isLoading = false
                )
            })
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // DELIVERY HELPER
    ///////////////////////////////////////////////////////////////////////////
    fun getOptimisedPath(addresses: List<String>, onSuccess: (OptimizedRouteWithOrders) -> Unit) {
        viewModelScope.launch {
            val dailyOrders = _orderScreenState.value.orders.filter {
                it.deliveryDate.toTimeStamp() == Instant.now().toEpochMilli()
            }
            val result = getOptimizedDeliveryUseCase.invoke(addresses, dailyOrders)
            onSuccess.invoke(result)
        }
    }

    fun updateShouldShowBatterieOptimization(shouldShow: Boolean) {
        viewModelScope.launch {
            shouldShowBatterieOptimizationUseCase.invoke(shouldShow = shouldShow)
        }
        _orderScreenState.value = _orderScreenState.value.copy(
            shouldShowBatterieOptimization = shouldShow
        )
    }

    fun getCurrentLocationToStart() {
        viewModelScope.launch {
            _orderScreenState.value = _orderScreenState.value.copy(
                currentAdminLocation = getCurrentLocationOnceUseCase.invoke()
            )
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // UI
    ///////////////////////////////////////////////////////////////////////////
    fun onLoading(isLoading: Boolean) {
        _orderScreenState.value = _orderScreenState.value.copy(
            isLoading = isLoading
        )
    }

    fun onError(error: String) {
        _orderScreenState.value = _orderScreenState.value.copy(
            error = error
        )
    }

    fun getTrackingStatus() {
        viewModelScope.launch {
            isInTrackingModeUseCase.invoke().collect { isInTrackingMode ->
                _orderScreenState.update {
                    it.copy(
                        isInTrackingMode = isInTrackingMode
                    )
                }
            }
        }
    }

    fun getTrackingStatusOnce(status: (Boolean) -> Unit) {
        viewModelScope.launch {
            isInTrackingModeUseCase.invoke().collect {
                status.invoke(it)
            }
        }
    }

}