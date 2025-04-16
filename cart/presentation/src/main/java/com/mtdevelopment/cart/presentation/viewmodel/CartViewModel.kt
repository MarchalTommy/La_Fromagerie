package com.mtdevelopment.cart.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class CartViewModel(
    getIsNetworkConnectedUseCase: GetIsNetworkConnectedUseCase,
    private val saveToDatastoreUseCase: SaveToDatastoreUseCase,
    private val getCartDataUseCase: GetCartDataUseCase
) : ViewModel(), KoinComponent {

    // TODO: Add a way to save a "preferred cart" with stuff you usually order

    val isConnected = getIsNetworkConnectedUseCase()

    var cartUiState by mutableStateOf(CartUiState())
        private set

    private fun setCartItems(value: CartItems) {
        cartUiState = cartUiState.copy(
            cartItems = value
        )
    }

    fun setCartVisibility(value: Boolean) {
        cartUiState = cartUiState.copy(
            isCartVisible = value
        )
    }

    fun addCartObject(valueAsUiObject: UiProductObject? = null, valueAsCartItem: CartItem? = null) {
        viewModelScope.launch {
            val cartItem = valueAsUiObject?.toCartItem() ?: valueAsCartItem

            var mutableContent =
                (cartUiState.cartItems?.cartItems as? MutableList) ?: mutableListOf()
            val selectedItem = mutableContent.find { it?.name == cartItem?.name }
            if (selectedItem != null) {
                selectedItem.quantity++
            } else if (mutableContent.isNotEmpty()) {
                // Looks weird but it's because I can't add to a list that does NOT exist...
                mutableContent.add(cartItem?.apply { quantity = 1 })
            } else {
                // So if it does not exist, I create it.
                mutableContent = mutableListOf(cartItem?.apply { quantity = 1 })
            }

            saveToDataStore(mutableContent.mapNotNull { it })
        }
    }

    fun removeCartObject(value: CartItem) {
        viewModelScope.launch {
            val mutableContent =
                (cartUiState.cartItems?.cartItems as? MutableList) ?: mutableListOf()
            val selectedItem = mutableContent.find { it?.name == value.name }
            if (selectedItem != null && selectedItem.quantity > 1) {
                selectedItem.quantity--
            }

            saveToDataStore(mutableContent.mapNotNull { it })
        }
    }

    fun totallyRemoveObject(value: CartItem) {
        viewModelScope.launch {
            val cleanedList =
                (cartUiState.cartItems?.cartItems as? MutableList) ?: mutableListOf()
            cleanedList.remove(value)

            saveToDataStore(cleanedList.mapNotNull { it })
        }
    }

    private fun saveToDataStore(cartContent: List<CartItem>) {
        val newCartObject = if (cartUiState.cartItems != null) {
            cartUiState.cartItems?.copy(
                cartItems = cartContent,
                totalPrice =
                    cartContent.sumOf { item ->
                        (item.price.times(item.quantity))
                    }
            )
        } else {
            CartItems(
                cartItems = cartContent,
                totalPrice =
                    cartContent.sumOf { item ->
                        (item.price.times(item.quantity))
                    }
            )
        }

        viewModelScope.launch {
            saveToDatastoreUseCase.invoke(
                CartItems(
                    newCartObject?.cartItems?.map {
                        CartItem(
                            name = it?.name ?: "",
                            price = it?.price ?: 0L,
                            quantity = it?.quantity ?: 0
                        )
                    } ?: emptyList(),
                    newCartObject?.totalPrice ?: 0L
                )
            )
            setCartItems(newCartObject ?: CartItems(emptyList(), 0L))
        }
    }

    fun saveClickedItem(value: UiProductObject) {
        cartUiState = cartUiState.copy(
            currentItem = value
        )
    }

    fun resetCart(withVisibility: Boolean = false) {
        setCartVisibility(!withVisibility)
        cartUiState.cartItems = null
    }

    fun loadCart(withVisibility: Boolean = false) {
        viewModelScope.launch {
            getCartDataUseCase.invoke().collect { data ->
                cartUiState = cartUiState.copy(
                    isCartVisible = withVisibility,
                    cartItems = CartItems(
                        cartItems = data?.cartItems ?: emptyList(),
                        totalPrice = data?.totalPrice ?: 0L
                    ),
                    currentItem = cartUiState.currentItem
                )
            }
        }
    }
}