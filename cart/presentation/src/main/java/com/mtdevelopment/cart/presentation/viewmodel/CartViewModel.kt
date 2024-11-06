package com.mtdevelopment.cart.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtdevelopment.cart.presentation.model.UiBasketObject
import com.mtdevelopment.cart.presentation.state.CartUiState
import com.mtdevelopment.core.model.CartItem
import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import com.mtdevelopment.core.usecase.SaveToDatastoreUseCase
import com.mtdevelopment.core.util.toCentsLong
import com.mtdevelopment.core.util.toUiPrice
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class CartViewModel(
    getIsNetworkConnectedUseCase: GetIsNetworkConnectedUseCase,
    private val saveToDatastoreUseCase: SaveToDatastoreUseCase
) : ViewModel(), KoinComponent {

    // TODO: Add a button to reset / empty the cart with confirmation popup
    // TODO: Add a way to save a "preferred cart" with stuff you usually order

    val isConnected = getIsNetworkConnectedUseCase()

    var cartUiState by mutableStateOf(CartUiState())
        private set

    private fun setCartObject(value: UiBasketObject) {
        cartUiState = cartUiState.copy(
            cartObject = value
        )
    }

    fun setCartVisibility(value: Boolean) {
        cartUiState = cartUiState.copy(
            isCartVisible = value
        )
    }

    fun addCartObject(value: UiProductObject) {
        viewModelScope.launch {

            var mutableContent = (cartUiState.cartObject.content as? MutableList) ?: mutableListOf()
            val selectedItem = mutableContent.find { it.id == value.id }
            if (selectedItem != null) {
                selectedItem.quantity++
            } else if (mutableContent.isNotEmpty()) {
                mutableContent.add(value.apply { quantity = 1 })
            } else {
                mutableContent = mutableListOf(value.apply { quantity = 1 })
            }

            val newCartObject = cartUiState.cartObject.copy(
                content = mutableContent,
                totalPrice =
                mutableContent.sumOf { (it.priceInCents * it.quantity) }.toUiPrice()
            )

            saveToDatastoreUseCase.invoke(
                CartItems(
                    newCartObject.content.map {
                        CartItem(
                            name = it.name,
                            price = it.priceInCents,
                            quantity = it.quantity
                        )
                    },
                    newCartObject.totalPrice
                        .replace(",", ".")
                        .replace("€", "")
                        .toDouble()
                        .toCentsLong()
                )
            )

            setCartObject(newCartObject)
        }
    }

    fun removeCartObject(value: UiProductObject) {
        viewModelScope.launch {
            val mutableContent = (cartUiState.cartObject.content as? MutableList) ?: mutableListOf()
            val selectedItem = mutableContent.find { it.id == value.id }
            if (selectedItem != null && selectedItem.quantity > 1) {
                selectedItem.quantity--
            }

            val newCartObject = cartUiState.cartObject.copy(
                content = mutableContent,
                totalPrice =
                mutableContent.sumOf { (it.priceInCents * it.quantity) }.toUiPrice()
            )

            saveToDatastoreUseCase.invoke(
                CartItems(
                    newCartObject.content.map {
                        CartItem(
                            name = it.name,
                            price = it.priceInCents,
                            quantity = it.quantity
                        )
                    },
                    newCartObject.totalPrice
                        .replace(",", ".")
                        .replace("€", "")
                        .toDouble()
                        .toCentsLong()
                )
            )

            setCartObject(newCartObject)
        }
    }

    fun totallyRemoveObject(value: UiProductObject) {
        viewModelScope.launch {

            val cleanedList = (cartUiState.cartObject.content as? MutableList) ?: mutableListOf()
            cleanedList.remove(value)

            val newCartObject = cartUiState.cartObject.copy(
                content = cleanedList,
                totalPrice =
                cleanedList.sumOf { (it.priceInCents * it.quantity) }.toUiPrice()
            )

            saveToDatastoreUseCase.invoke(
                CartItems(
                    newCartObject.content.map {
                        CartItem(
                            name = it.name,
                            price = it.priceInCents,
                            quantity = it.quantity
                        )
                    },
                    newCartObject.totalPrice
                        .replace(",", ".")
                        .replace("€", "")
                        .toDouble()
                        .toCentsLong()
                )
            )

            setCartObject(newCartObject)
        }
    }
}