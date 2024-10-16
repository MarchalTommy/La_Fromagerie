package com.mtdevelopment.lafromagerie.navigation

import com.mtdevelopment.cart.presentation.model.UiBasketObject
import com.mtdevelopment.checkout.presentation.model.UiCheckoutObject
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import kotlinx.serialization.Serializable


@Serializable
object HomeScreen

@Serializable
data class DetailDestination(
    val productObject: UiProductObject
)

@Serializable
data class DeliveryOptionScreen(
    val cartItems: UiBasketObject
)

@Serializable
data class CheckoutScreen(
    val checkoutData: UiCheckoutObject
)