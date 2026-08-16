package com.mtdevelopment.checkout.domain.model

import androidx.annotation.Keep
import com.mtdevelopment.core.model.CartItems

@Keep
data class LocalCheckoutInformation(
    val buyerName: String? = null,
    val buyerAddress: String? = null,
    val buyerEmail: String? = null,
    val cartItems: CartItems? = null,
    val totalPrice: Long? = null,
    val deliveryDate: Long? = null,
    val billingAddress: String? = null,
    val buyerPhone: String? = null,
    /** [com.mtdevelopment.core.model.FulfillmentType] name; null reads as DELIVERY. */
    val fulfillmentType: String? = null,
    val pickupPointId: String? = null,
    val pickupLabel: String? = null,
    val pickupAddress: String? = null,
    val pickupTimeRange: String? = null
)
