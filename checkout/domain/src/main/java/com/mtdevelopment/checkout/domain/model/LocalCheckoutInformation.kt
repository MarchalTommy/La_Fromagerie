package com.mtdevelopment.checkout.domain.model

import com.mtdevelopment.core.model.CartItems

data class LocalCheckoutInformation(
    val buyerName: String,
    val buyerAddress: String,
    val cartItems: CartItems,
    val totalPrice: Long,
    val deliveryDate: Long
)
