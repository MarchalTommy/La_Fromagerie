package com.mtdevelopment.cart.presentation.state

import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject

data class CartUiState(
    val isCartVisible: Boolean = false,
    var cartItems: CartItems? = CartItems(
        emptyList(),
        0L
    ),
    val currentItem: UiProductObject? = null
)
