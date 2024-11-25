package com.mtdevelopment.checkout.domain.model

import androidx.annotation.Keep
import com.mtdevelopment.core.model.CartItems

@Keep
data class LocalCheckoutInformation(
    val buyerName: String? = null,
    val buyerAddress: String? = null,
    val cartItems: CartItems? = null,
    val totalPrice: Long? = null,
    val deliveryDate: Long? = null
)
