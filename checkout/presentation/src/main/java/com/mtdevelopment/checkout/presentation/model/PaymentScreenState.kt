package com.mtdevelopment.checkout.presentation.model

import com.mtdevelopment.checkout.domain.model.NewCheckoutResult
import com.mtdevelopment.core.model.CartItems

data class PaymentScreenState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isPaymentSuccess: Boolean = false,
    val isGooglePayAvailable: Boolean = false,
    val buyerName: String? = null,
    val buyerAddress: String? = null,
    val buyerBillingAddress: String? = null,
    val cartItems: CartItems? = null,
    val totalPrice: Long? = null,
    val deliveryDate: Long? = null,
    val checkoutResult: NewCheckoutResult? = null,
    var orderId: String? = null,
    var checkoutNote: String? = null
)
