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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
     * The mode the customer is shopping in. Collected so the catalogue re-prices itself the
     * moment they switch, rather than showing delivery prices up to the payment screen.
     */
    private val fulfillmentType: StateFlow<FulfillmentType> =
        sharedDatastore.fulfillmentTypeFlow
            .map { FulfillmentType.fromStoredValue(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = FulfillmentType.DELIVERY
            )

    /**
     * Flow observing network connectivity.
     */
    val isConnected = getIsNetworkConnectedUseCase()

    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    /**
     * Last catalogue loaded, kept in its domain form. The UI state only holds prices already
     * resolved for one mode, so re-pricing needs the products that still carry both.
     */
    private var loadedProducts: List<Product> = emptyList()

    init {
        // Initialization: start the sync check and initial load
        checkAndUpdateDatabase()

        // Re-price whenever the customer switches mode. This lives here, and not in the cart,
        // because pricing is a property of the catalogue: HomeViewModel is the only place that
        // already holds both the products and the datastore, so routing it through the cart
        // would mean making that module depend on the catalogue for nothing.
        viewModelScope.launch {
            // drop(1): the initial emission is the mode the catalogue was just loaded with,
            // so reacting to it would fetch the products a second time on every launch. Only
            // an actual switch needs re-pricing.
            fulfillmentType.drop(1).collect { type ->
                refreshPricesFor(type)
            }
        }
    }

    /**
     * Re-prices the catalogue and the basket for [type].
     *
     * A cart line stores the unit price it was added at, so a mode switch after the basket is
     * filled would otherwise charge the old prices — precisely the surprise this feature must
     * never produce. Lines are matched **by name**, already the join key the orders collection
     * uses; a product that has since left the catalogue keeps its stored price rather than
     * silently vanishing from the basket.
     */
    private suspend fun refreshPricesFor(type: FulfillmentType) {
        // Re-priced from the products already in hand rather than re-fetched: the prices are
        // in memory, and a network round-trip to restate them would be wasted work. Nothing
        // loaded yet means nothing to re-price.
        val products = loadedProducts.takeIf { it.isNotEmpty() } ?: return

        _homeUiState.update { state ->
            state.copy(products = products.map { it.toUiProductObject(type) })
        }

        val cart = sharedDatastore.cartItemsFlow.firstOrNull() ?: return
        val catalogue = products.associateBy { it.name }
        val repriced = cart.cartItems.filterNotNull().map { item ->
            val product = catalogue[item.name] ?: return@map item
            item.copy(price = product.priceFor(type))
        }
        if (repriced != cart.cartItems.filterNotNull()) {
            sharedDatastore.setCartItems(
                cart.copy(
                    cartItems = repriced,
                    totalPrice = repriced.sumOf { it.price * it.quantity }
                )
            )
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
                    loadedProducts = result.data
                    _homeUiState.update {
                        it.copy(
                            products = result.data.map { p ->
                                p.toUiProductObject(fulfillmentType.value)
                            },
                            isLoading = false,
                            isError = null
                        )
                    }
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