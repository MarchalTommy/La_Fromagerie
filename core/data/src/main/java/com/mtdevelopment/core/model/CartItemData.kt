package com.mtdevelopment.core.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
@Keep
data class CartItemData(
    @SerializedName("name")
    val name: String,
    @SerializedName("price")
    val price: Long,
    @SerializedName("quantity")
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