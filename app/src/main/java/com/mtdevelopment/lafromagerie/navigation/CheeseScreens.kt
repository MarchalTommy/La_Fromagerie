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
object CheckoutScreenDestination