package com.mtdevelopment.core.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class CartItems(
    @SerializedName("cartItems")
    val cartItems: List<CartItem?>,
    @SerializedName("totalPrice")
    val totalPrice: Long
)
