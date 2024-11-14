package com.mtdevelopment.home.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtdevelopment.core.presentation.sharedModels.ProductType
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import com.mtdevelopment.home.domain.model.Product
import com.mtdevelopment.home.domain.usecase.AddNewProductUseCase
import com.mtdevelopment.home.domain.usecase.DeleteProductUseCase
import com.mtdevelopment.home.domain.usecase.GetAllCheesesUseCase
import com.mtdevelopment.home.domain.usecase.GetAllProductsUseCase
import com.mtdevelopment.home.domain.usecase.UpdateProductUseCase
import com.mtdevelopment.home.presentation.state.HomeUiState
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class HomeViewModel(
    private val getAllProductsUseCase: GetAllProductsUseCase,
    private val getAllCheesesUseCase: GetAllCheesesUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val addNewProductUseCase: AddNewProductUseCase,
    private val getIsNetworkConnectedUseCase: GetIsNetworkConnectedUseCase
) : ViewModel(), KoinComponent {

    val isConnected = getIsNetworkConnectedUseCase()

    var homeUiState by mutableStateOf(HomeUiState())
        private set

    init {
        viewModelScope.launch {
            getAllProducts()
        }
    }

    private fun getAllProducts() {
        getAllProductsUseCase.invoke(
            onSuccess = { products ->
                homeUiState = homeUiState.copy(products = products.map { it.toUiProductObject() })
            },
            onFailure = {
                // TODO: HANDLE ERROR
            }
        )
    }

    fun updateProduct(product: UiProductObject) {
        updateProductUseCase.invoke(product.toDomainProduct())
    }

    fun deleteProduct(product: UiProductObject) {
        deleteProductUseCase.invoke(product.toDomainProduct())
    }

    fun addNewProduct(product: UiProductObject) {
        addNewProductUseCase.invoke(product.toDomainProduct())
    }

    private fun Product.toUiProductObject() = UiProductObject(
        id = id,
        name = name,
        priceInCents = priceInCents,
        imageUrl = imageUrl,
        type = ProductType.valueOf(type),
        description = description,
        allergens = allergens
    )

    private fun UiProductObject.toDomainProduct() = Product(
        id = id,
        name = name,
        priceInCents = priceInCents,
        imageUrl = imageUrl!!,
        type = type.name,
        description = description,
        allergens = allergens
    )
}