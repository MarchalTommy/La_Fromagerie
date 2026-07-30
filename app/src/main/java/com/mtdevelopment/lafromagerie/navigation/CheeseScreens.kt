package com.mtdevelopment.lafromagerie.navigation

import kotlinx.serialization.Serializable


/**
 * Navigation destination for the Home/Catalog screen.
 * @property shouldRefresh True if the screen should force a refresh of the product list upon entry.
 */
@Serializable
data class HomeScreenDestination(
    val shouldRefresh: Boolean = false
)

/**
 * Navigation destination for the Product Detail screen.
 */
@Serializable
object DetailScreenDestination

/**
 * Navigation destination for the Delivery configuration screen (Client side).
 */
@Serializable
object DeliveryOptionScreenDestination

/**
 * Navigation destination for the Delivery Helper/Route optimization screen (Admin side).
 */
@Serializable
object DeliveryHelperScreenDestination

/**
 * Navigation destination for the Order Preparation screen (Admin side).
 */
@Serializable
object OrdersScreenDestination

/**
 * Navigation destination for the delivery path editor (Admin side).
 *
 * Registered only in the admin navigation graph; the client one never reaches it.
 *
 * @property pathId Path to edit, or null to create a new one.
 */
@Serializable
data class PathEditScreenDestination(
    val pathId: String? = null
)

/**
 * Navigation destination for the Checkout and Payment screen.
 */
@Serializable
object CheckoutScreenDestination

/**
 * Navigation destination for the Post-Payment Success screen.
 * @property clientName The buyer's name for personalizing the success message.
 */
@Serializable
data class AfterPaymentScreenDestination(
    val clientName: String? = null
)