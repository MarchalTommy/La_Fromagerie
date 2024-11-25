package com.mtdevelopment.core.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class CartItem(
    @SerializedName("name")
    val name: String,
    @SerializedName("price")
    val price: Long,
    @SerializedName("quantity")
    val quantity: Int
)
