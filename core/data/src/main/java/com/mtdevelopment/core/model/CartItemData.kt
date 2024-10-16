package com.mtdevelopment.core.model

import kotlinx.serialization.Serializable

@Serializable
data class CartItemData(
    val name: String,
    val price: Long,
    val quantity: Int
)

// Data to domain
fun CartItemData.toCartItem(): CartItem {
    return CartItem(
        name = this.name,
        price = this.price,
        quantity = this.quantity
    )
}

// Domain to data
fun CartItem.toCartItemData(): CartItemData {
    return CartItemData(
        name = this.name,
        price = this.price,
        quantity = this.quantity
    )
}