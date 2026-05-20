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
import com.mtdevelopment.admin.domain.usecase.GetPreparationStatusesUseCase
import com.mtdevelopment.admin.domain.usecase.GetShouldShowBatterieOptimizationUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateDeliveryPathUseCase
import com.mtdevelopment.admin.domain.usecase.UpdatePreparationStatusUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateProductUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateShouldShowBatterieOptimizationUseCase
import com.mtdevelopment.admin.domain.usecase.UploadImageUseCase
import com.mtdevelopment.admin.presentation.model.OrderScreenState
import com.mtdevelopment.core.domain.toTimeStamp
import com.mtdevelopment.core.model.AutoCompleteSuggestion
import com.mtdevelopment.core.model.DeliveryPath
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.PreparationStatus
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.presentation.sharedModels.toDomainProduct
import com.mtdevelopment.core.usecase.GetAutocompleteSuggestionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import java.time.Instant

/**
 * ViewModel for the administrative part of the application.
 * It manages various features:
 * - Product management (add, update, delete with image upload).
 * - Delivery path management (CRUD operations on paths).
 * - Order management (retrieving and updating preparation statuses).
 * - Delivery helper (route optimization and tracking).
 * - Address autocomplete for adding new orders manually.
 */
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
    private val getPreparationStatusesUseCase: GetPreparationStatusesUseCase,
    private val updatePreparationStatusUseCase: UpdatePreparationStatusUseCase
) : ViewModel(), KoinComponent {

    ///////////////////////////////////////////////////////////////////////////
    // UI State
    ///////////////////////////////////////////////////////////////////////////
    private val _orderScreenState = MutableStateFlow(OrderScreenState())
    val orderScreenState = _orderScreenState.asStateFlow()

    /**
     * Internal state for debouncing address autocomplete queries.
     */
    private val _searchQuery = MutableStateFlow("")

    init {
        // Observe battery optimization preference
        viewModelScope.launch {
            getShouldShowBatterieOptimizationUseCase.invoke().collect {
                _orderScreenState.value = _orderScreenState.value.copy(
                    shouldShowBatterieOptimization = it
                )
            }
        }

        // Debounced search for address suggestions
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Avoid flooding the API
                .distinctUntilChanged()
                .filter { it.isNotBlank() && it.length >= 2 }
                .collectLatest { query ->
                    fetchAddressSuggestions(query)
                }
        }

        // Manage suggestion list visibility based on query
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
    // Address Autocomplete
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
                println("Error fetching suggestions: ${e.message}")
            }
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

    ///////////////////////////////////////////////////////////////////////////
    // Product Operations
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Updates a product. If a local image URI is provided, it is uploaded to Cloudinary first.
     */
    fun updateProduct(
        product: UiProductObject,
        onLoading: (Boolean) -> Unit,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            onLoading.invoke(true)
            // Handle image upload if it's a local URI
            // // TODO: If image upload fails, should we still proceed with product update?
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
            deleteProductUseCase.invoke(product.toDomainProduct(), onSuccess = {}, onError = {})
        }
    }

    /**
     * Adds a new product. If a local image URI is provided, it is uploaded to Cloudinary first.
     */
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
    // Delivery Path Operations
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
    // Order and Status Operations
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Fetches all orders and preparation statuses.
     */
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
            getPreparationStatusesUseCase.invoke(onSuccess = {
                _orderScreenState.update { state ->
                    state.copy(preparationStatuses = it ?: emptyList())
                }
            })
        }
    }

    /**
     * Manually adds a temporary order to the delivery helper screen state.
     */
    fun addOrder(order: Order) {
        viewModelScope.launch {
            _orderScreenState.update { state ->
                state.copy(orders = state.orders + order)
            }
        }
    }

    /**
     * Updates a preparation status with optimistic UI update.
     */
    fun updatePreparationStatus(status: PreparationStatus) {
        viewModelScope.launch {
            // Optimistic update
            _orderScreenState.update { state ->
                val newStatuses = state.preparationStatuses.toMutableList()
                val index = newStatuses.indexOfFirst { it.id == status.id }
                if (index != -1) {
                    newStatuses[index] = status
                } else {
                    newStatuses.add(status)
                }
                state.copy(preparationStatuses = newStatuses)
            }
            updatePreparationStatusUseCase.invoke(status)
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Delivery Helper & Tracking
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Calculates an optimized path for today's orders.
     */
    fun getOptimisedPath(addresses: List<String>, onSuccess: (OptimizedRouteWithOrders) -> Unit) {
        viewModelScope.launch {
            // // TODO: 'toTimeStamp' and date comparison needs to be robust against timezone issues.
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

    fun getTrackingStatusOnce(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult.invoke(isInTrackingModeUseCase.invoke().first())
        }
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

    ///////////////////////////////////////////////////////////////////////////
    // General UI Management
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

    fun setDialogVisibility(isVisible: Boolean) {
        _orderScreenState.value = _orderScreenState.value.copy(shouldShowDialog = isVisible)
    }
}