package com.mtdevelopment.lafromagerie.navigation

import kotlinx.serialization.Serializable


@Serializable
data class HomeScreenDestination(
    val shouldRefresh: Boolean = false
)

@Serializable
object DetailScreenDestination

@Serializable
object DeliveryOptionScreenDestination

@Serializable
object DeliveryHelperScreenDestination

@Serializable
object OrdersScreenDestination

@Serializable
object CheckoutScreenDestination

@Serializable
data class AfterPaymentScreenDestination(
    val clientName: String? = null
)