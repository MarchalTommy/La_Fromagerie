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
import com.mtdevelopment.home.domain.usecase.GetLastFirestoreDatabaseUpdateUseCase
import com.mtdevelopment.home.presentation.state.HomeUiState
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

/**
 * ViewModel for the Home screen, responsible for displaying the product catalog.
 * It manages the initial synchronization check with Firestore and coordinates 
 * the fetching of products from either the local cache or the remote server.
 */
class HomeViewModel(
    private val getAllProductsUseCase: GetAllProductsUseCase,
    private val getAllCheesesUseCase: GetAllCheesesUseCase,
    private val getLastFirestoreDatabaseUpdateUseCase: GetLastFirestoreDatabaseUpdateUseCase,
    getIsNetworkConnectedUseCase: GetIsNetworkConnectedUseCase
) : ViewModel(), KoinComponent {

    /**
     * Flow observing network connectivity.
     */
    val isConnected = getIsNetworkConnectedUseCase()

    /**
     * The UI state for the Home screen.
     */
    var homeUiState by mutableStateOf(HomeUiState())
        private set

    init {
        // Initialization: start the sync check and initial load
        viewModelScope.launch {
            checkAndUpdateDatabase()
        }
    }

    /**
     * Orchestrates the database update check.
     * It first queries the server for the last update timestamps. 
     * If a change is detected, flags are set in Datastore, and [getAllProducts] 
     * will then perform the full synchronization.
     */
    private suspend fun checkAndUpdateDatabase() {
        getLastFirestoreDatabaseUpdateUseCase.invoke(onSuccess = {
            viewModelScope.launch {
                // Once synchronization check is done, fetch the products
                getAllProducts()
            }
        }, onFailure = {
            homeUiState = homeUiState.copy(
                isLoading = false,
                isError = "Mise à jour de la base de donnée impossible"
            )
        })
    }

    /**
     * Fetches all products using the [GetAllProductsUseCase].
     * The use case handles whether to fetch from local Room or remote Firestore 
     * based on the flags set during the update check.
     */
    private suspend fun getAllProducts() {
        getAllProductsUseCase.invoke(
            scope = viewModelScope,
            onSuccess = { products ->
                homeUiState = homeUiState.copy(
                    // Map domain [Product] to presentation [UiProductObject]
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

    /**
     * Manually triggers a refresh of the product list.
     */
    fun refreshProducts() {
        homeUiState = homeUiState.copy(isLoading = true)
        viewModelScope.launch {
            checkAndUpdateDatabase()
        }
    }

    fun setIsLoading(isLoading: Boolean) {
        homeUiState = homeUiState.copy(isLoading = isLoading)
    }
}