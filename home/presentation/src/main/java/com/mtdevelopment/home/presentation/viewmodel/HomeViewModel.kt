package com.mtdevelopment.home.presentation.viewmodel

import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtdevelopment.core.presentation.sharedModels.toUiProductObject
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import com.mtdevelopment.home.domain.usecase.GetAllCheesesUseCase
import com.mtdevelopment.home.domain.usecase.GetAllProductsUseCase
import com.mtdevelopment.home.domain.usecase.GetLastFirestoreDatabaseUpdateUseCase
import com.mtdevelopment.home.presentation.state.HomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class HomeViewModel(
    private val getAllProductsUseCase: GetAllProductsUseCase,
    private val getAllCheesesUseCase: GetAllCheesesUseCase,
    private val getLastFirestoreDatabaseUpdateUseCase: GetLastFirestoreDatabaseUpdateUseCase,
    getIsNetworkConnectedUseCase: GetIsNetworkConnectedUseCase
) : ViewModel(), KoinComponent {

    val isConnected = getIsNetworkConnectedUseCase()

    var homeUiState by mutableStateOf(HomeUiState())
        private set

    private val _isRefreshReady: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val isRefreshReady: StateFlow<Boolean> = _isRefreshReady.asStateFlow()

    init {
        viewModelScope.launch {
            getLastFirestoreDatabaseUpdateUseCase.invoke(onSuccess = {
                this.launch {
                    getAllProducts()
                }
            }, onFailure = {
                homeUiState = homeUiState.copy(
                    isLoading = false,
                    isError = "Mise à jour de la base de donnée impossible"
                )
            })
        }
    }

    private suspend fun getAllProducts() {
        Log.i(
            TAG,
            "NOW GETTING ALL PRODUCTS"
        )

        getAllProductsUseCase.invoke(
            scope = viewModelScope,
            onSuccess = { products ->
                Log.i(
                    TAG,
                    "PRODUCTS FETCHED : $products"
                )
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