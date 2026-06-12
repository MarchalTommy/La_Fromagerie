package com.mtdevelopment.cart.presentation.state

import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject

/**
 * Represents the UI state for the shopping cart.
 * @property isCartVisible Whether the cart overlay or screen is currently visible.
 * @property cartItems The current list of items in the cart and the total amount.
 * @property currentItem The product currently being viewed or modified within the cart context.
 */
data class CartUiState(
    val isCartVisible: Boolean = false,
    val cartItems: CartItems? = CartItems(
        emptyList(),
        0L
    ),
    val currentItem: UiProductObject? = null
)
