package com.mtdevelopment.core.model

import kotlinx.serialization.Serializable

@Serializable
data class CartItemsData(
    val cartItems: List<CartItemData>,
    val totalPrice: Long
)

// Data to domain
fun CartItemsData.toCartItems(): CartItems {
    return CartItems(
        cartItems = this.cartItems.map { it.toCartItem() },
        totalPrice = this.totalPrice
    )
}

// Domain to data
fun CartItems.toCartItemsData(): CartItemsData {
    return CartItemsData(
        cartItems = this.cartItems.map { it.toCartItemData() },
        totalPrice = this.totalPrice
    )
}