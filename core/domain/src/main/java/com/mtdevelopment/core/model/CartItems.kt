package com.mtdevelopment.core.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

/**
 * Domain model representing the entire content of the shopping cart.
 * @property cartItems The list of [CartItem] currently in the cart.
 * @property totalPrice The total cost of all items in the cart.
 */
@Keep
@Serializable
data class CartItems(
    @SerializedName("cartItems")
    val cartItems: List<CartItem?>,
    @SerializedName("totalPrice")
    val totalPrice: Long
)
