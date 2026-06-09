package com.mtdevelopment.cart.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtdevelopment.cart.domain.usecase.GetCartDataUseCase
import com.mtdevelopment.cart.presentation.state.CartUiState
import com.mtdevelopment.core.model.CartItem
import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.presentation.sharedModels.toCartItem
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import com.mtdevelopment.core.usecase.SaveToDatastoreUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

/**
 * ViewModel responsible for managing the shopping cart state and operations.
 * It handles adding, removing, and updating product quantities, as well as persisting the cart to DataStore.
 */
class CartViewModel(
    getIsNetworkConnectedUseCase: GetIsNetworkConnectedUseCase,
    private val saveToDatastoreUseCase: SaveToDatastoreUseCase,
    private val getCartDataUseCase: GetCartDataUseCase
) : ViewModel(), KoinComponent {

    // TODO: Add a way to save a "preferred cart" with stuff you usually order

    /**
     * Flow indicating the current network connection status.
     */
    val isConnected = getIsNetworkConnectedUseCase()

    /**
     * The current UI state of the cart as a read-only StateFlow.
     */
    private val _cartUiState = MutableStateFlow(CartUiState())
    val cartUiState: StateFlow<CartUiState> = _cartUiState.asStateFlow()

    init {
        // Keep the in-memory cart in sync with the DataStore for the whole ViewModel lifetime:
        // restores the persisted cart at startup and reflects external changes
        // (e.g. the cart being cleared after a successful payment).
        viewModelScope.launch {
            getCartDataUseCase.invoke().collect { data ->
                _cartUiState.update {
                    it.copy(
                        cartItems = CartItems(
                            cartItems = data?.cartItems ?: emptyList(),
                            totalPrice = data?.totalPrice ?: 0L
                        )
                    )
                }
            }
        }
    }

    private fun setCartItems(value: CartItems) {
        _cartUiState.update {
            it.copy(cartItems = value)
        }
    }

    /**
     * Updates the visibility of the cart UI.
     */
    fun setCartVisibility(value: Boolean) {
        _cartUiState.update {
            it.copy(isCartVisible = value)
        }
    }

    /**
     * Adds a product to the cart. If the item already exists, its quantity is incremented.
     * @param valueAsUiObject The product to add, provided as a UI model.
     * @param valueAsCartItem The product to add, provided as a cart item model.
     */
    fun addCartObject(valueAsUiObject: UiProductObject? = null, valueAsCartItem: CartItem? = null) {
        viewModelScope.launch {
            val incomingItem = valueAsUiObject?.toCartItem() ?: valueAsCartItem ?: return@launch

            val currentItems =
                _cartUiState.value.cartItems?.cartItems?.filterNotNull() ?: emptyList()
            val mutableContent = currentItems.toMutableList()

            val existingItemIndex = mutableContent.indexOfFirst { it.name == incomingItem.name }

            if (existingItemIndex != -1) {
                // Item already in cart, create a new copy with incremented quantity
                val existingItem = mutableContent[existingItemIndex]
                mutableContent[existingItemIndex] =
                    existingItem.copy(quantity = existingItem.quantity + 1)
            } else {
                // New item, add a copy with quantity 1
                mutableContent.add(incomingItem.copy(quantity = 1))
            }

            saveToDataStore(mutableContent)
        }
    }

    /**
     * Decrements the quantity of an item in the cart.
     * If quantity reaches 1, it stays in the cart unless [totallyRemoveObject] is called.
     */
    fun removeCartObject(value: CartItem) {
        viewModelScope.launch {
            val currentItems =
                _cartUiState.value.cartItems?.cartItems?.filterNotNull() ?: emptyList()
            val mutableContent = currentItems.toMutableList()

            val existingItemIndex = mutableContent.indexOfFirst { it.name == value.name }

            if (existingItemIndex != -1) {
                val existingItem = mutableContent[existingItemIndex]
                if (existingItem.quantity > 1) {
                    mutableContent[existingItemIndex] =
                        existingItem.copy(quantity = existingItem.quantity - 1)
                }
            }

            saveToDataStore(mutableContent)
        }
    }

    /**
     * Removes an item completely from the cart regardless of its quantity.
     */
    fun totallyRemoveObject(value: CartItem) {
        viewModelScope.launch {
            val currentItems =
                _cartUiState.value.cartItems?.cartItems?.filterNotNull() ?: emptyList()
            val filteredList = currentItems.filterNot { it.name == value.name }

            saveToDataStore(filteredList)
        }
    }

    /**
     * Persists the current cart content to the DataStore and updates the UI state.
     * Also calculates the total price of the cart.
     */
    private suspend fun saveToDataStore(cartContent: List<CartItem>) {
        val currentCartItems = _cartUiState.value.cartItems
        val totalPrice = cartContent.sumOf { it.price * it.quantity }
        val newCartObject = currentCartItems?.copy(
            cartItems = cartContent,
            totalPrice = totalPrice
        ) ?: CartItems(cartContent, totalPrice)

        saveToDatastoreUseCase.invoke(
            CartItems(
                newCartObject.cartItems.map {
                    CartItem(
                        name = it?.name ?: "",
                        price = it?.price ?: 0L,
                        quantity = it?.quantity ?: 0
                    )
                },
                newCartObject.totalPrice
            )
        )
        setCartItems(newCartObject)
    }

    /**
     * Saves the product currently being interacted with in the UI.
     */
    fun saveClickedItem(value: UiProductObject) {
        _cartUiState.update {
            it.copy(currentItem = value)
        }
    }

}