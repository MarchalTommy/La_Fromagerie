package com.mtdevelopment.core.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
@Keep
data class CartItemsData(
    @SerializedName("cartItems")
    val cartItems: List<CartItemData>,

    @SerializedName("totalPrice")
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