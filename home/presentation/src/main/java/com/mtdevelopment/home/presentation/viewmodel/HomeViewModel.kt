package com.mtdevelopment.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtdevelopment.core.presentation.R
import com.mtdevelopment.core.model.FulfillmentType
import com.mtdevelopment.core.model.Product
import com.mtdevelopment.core.presentation.sharedModels.toUiProductObject
import com.mtdevelopment.core.repository.SharedDatastore
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import com.mtdevelopment.core.util.DataResult
import com.mtdevelopment.core.util.UiText
import com.mtdevelopment.home.domain.usecase.GetAllCheesesUseCase
import com.mtdevelopment.home.domain.usecase.GetAllProductsUseCase
import com.mtdevelopment.home.domain.usecase.GetLastFirestoreDatabaseUpdateUseCase
import com.mtdevelopment.home.presentation.state.HomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
    getIsNetworkConnectedUseCase: GetIsNetworkConnectedUseCase,
    private val sharedDatastore: SharedDatastore
) : ViewModel(), KoinComponent {

    /**
     * Flow observing network connectivity.
     */
    val isConnected = getIsNetworkConnectedUseCase()

    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    /**
     * The catalogue as loaded, still carrying every mode's price.
     *
     * Display-only. The basket used to be re-priced from here whenever the mode changed, which
     * made the amount charged depend on this ViewModel being alive and loaded; the basket now
     * resolves its own prices on read, so nothing outside this screen depends on this list.
     */
    private val loadedProducts = MutableStateFlow<List<Product>>(emptyList())

    init {
        // Initialization: start the sync check and initial load
        checkAndUpdateDatabase()

        // The catalogue shows the price of the mode in force, so the customer never fills a
        // basket against prices they will not be charged. Derived straight from the two inputs
        // rather than repaired on change: there is no third copy of the prices to go stale.
        viewModelScope.launch {
            combine(
                loadedProducts,
                sharedDatastore.fulfillmentTypeFlow.map { FulfillmentType.fromStoredValue(it) }
            ) { products, mode ->
                products.map { it.toUiProductObject(mode) }
            }.collect { products ->
                _homeUiState.update { it.copy(products = products) }
            }
        }
    }

    /**
     * Orchestrates the database update check.
     * It first queries the server for the last update timestamps. 
     * If a change is detected, flags are set in Datastore, and [getAllProducts] 
     * will then perform the full synchronization.
     */

    private fun checkAndUpdateDatabase() {
        viewModelScope.launch {
            _homeUiState.update { it.copy(isLoading = true) }

            getLastFirestoreDatabaseUpdateUseCase.invoke(onSuccess = {
                getAllProducts()
            }, onFailure = {
                _homeUiState.update {
                    it.copy(
                        isLoading = false,
                        isError = UiText.StringResource(R.string.error_database_update)
                    )
                }
            })
        }
    }

    /**
     * Fetches all products using the [GetAllProductsUseCase].
     * The use case handles whether to fetch from local Room or remote Firestore 
     * based on the flags set during the update check.
     */
    private fun getAllProducts(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            when (val result = getAllProductsUseCase(forceRefresh)) {
                is DataResult.Success -> {
                    loadedProducts.value = result.data
                    _homeUiState.update { it.copy(isLoading = false, isError = null) }
                }

                is DataResult.Error -> {
                    _homeUiState.update {
                        it.copy(
                            isLoading = false,
                            isError = result.message?.let { msg -> UiText.DynamicString(msg) }
                                ?: UiText.StringResource(R.string.error_loading_products)
                        )
                    }
                }

                is DataResult.Loading -> {
                    _homeUiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    /**
     * Manually triggers a refresh of the product list.
     */
    fun refreshProducts() {
        getAllProducts(forceRefresh = true)
    }

    fun setIsLoading(isLoading: Boolean) {
        _homeUiState.update { it.copy(isLoading = isLoading) }
    }
}