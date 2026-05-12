package com.mtdevelopment.core.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

/**
 * Domain model representing an item in the shopping cart.
 * @property name The product name.
 * @property price The unit price of the product.
 * @property quantity The number of units in the cart.
 */
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
