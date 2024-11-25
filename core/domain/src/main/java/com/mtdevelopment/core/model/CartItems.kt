package com.mtdevelopment.core.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class CartItems(
    @SerializedName("cartItems")
    val cartItems: List<CartItem>,
    @SerializedName("totalPrice")
    val totalPrice: Long
)
