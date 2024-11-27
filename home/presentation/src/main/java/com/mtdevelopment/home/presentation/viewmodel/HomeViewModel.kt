package com.mtdevelopment.home.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtdevelopment.core.presentation.sharedModels.toUiProductObject
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import com.mtdevelopment.home.domain.usecase.GetAllCheesesUseCase
import com.mtdevelopment.home.domain.usecase.GetAllProductsUseCase
import com.mtdevelopment.home.presentation.state.HomeUiState
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class HomeViewModel(
    private val getAllProductsUseCase: GetAllProductsUseCase,
    private val getAllCheesesUseCase: GetAllCheesesUseCase,
    getIsNetworkConnectedUseCase: GetIsNetworkConnectedUseCase
) : ViewModel(), KoinComponent {

    val isConnected = getIsNetworkConnectedUseCase()

    var homeUiState by mutableStateOf(HomeUiState())
        private set

    init {
        viewModelScope.launch {
            getAllProducts()
        }
    }

    private suspend fun getAllProducts() {
        getAllProductsUseCase.invoke(
            scope = viewModelScope,
            onSuccess = { products ->
                homeUiState = homeUiState.copy(
                    products = products.map { it.toUiProductObject() },
                    isLoading = false
                )
            },
            onFailure = {
                homeUiState = homeUiState.copy(
                    isLoading = false,
                    isError = "Chargement des produits impossible"
                )
            }
        )
    }

    fun refreshProducts() {
        homeUiState = homeUiState.copy(isLoading = true)
        viewModelScope.launch {
            getAllProducts()
        }
    }
}