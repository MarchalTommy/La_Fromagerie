package com.mtdevelopment.core.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class CartItem(
    @SerializedName("name")
    val name: String,
    @SerializedName("price")
    val price: Long,
    @SerializedName("quantity")
    var quantity: Int
)
