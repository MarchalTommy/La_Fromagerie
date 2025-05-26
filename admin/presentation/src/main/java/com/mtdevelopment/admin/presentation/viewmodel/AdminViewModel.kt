package com.mtdevelopment.admin.presentation.viewmodel

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtdevelopment.admin.domain.usecase.AddNewPathUseCase
import com.mtdevelopment.admin.domain.usecase.AddNewProductUseCase
import com.mtdevelopment.admin.domain.usecase.DeletePathUseCase
import com.mtdevelopment.admin.domain.usecase.DeleteProductUseCase
import com.mtdevelopment.admin.domain.usecase.GetAllOrdersUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateDeliveryPathUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateProductUseCase
import com.mtdevelopment.admin.domain.usecase.UploadImageUseCase
import com.mtdevelopment.admin.presentation.model.OrderScreenState
import com.mtdevelopment.core.model.DeliveryPath
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.presentation.sharedModels.toDomainProduct
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class AdminViewModel(
    private val updateProductUseCase: UpdateProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val addNewProductUseCase: AddNewProductUseCase,
    private val updateDeliveryPathUseCase: UpdateDeliveryPathUseCase,
    private val deleteDeliveryPathUseCase: DeletePathUseCase,
    private val addNewDeliveryPathUseCase: AddNewPathUseCase,
    private val getAllOrdersUseCase: GetAllOrdersUseCase,
    private val uploadImageUseCase: UploadImageUseCase,
) : ViewModel(), KoinComponent {

    ///////////////////////////////////////////////////////////////////////////
    // States
    ///////////////////////////////////////////////////////////////////////////
    private val _orderScreenState = MutableStateFlow(OrderScreenState())
    val orderScreenState = _orderScreenState.asStateFlow()

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

    fun getOptimisedPath(addresses: List<String>) {

    }

}