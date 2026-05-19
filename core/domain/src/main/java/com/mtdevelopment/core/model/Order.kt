package com.mtdevelopment.core.model

import kotlinx.serialization.Serializable

/**
 * Core domain model representing a customer order.
 * 
 * @property id Unique identifier (generated during checkout).
 * @property customerName Full name of the buyer.
 * @property customerAddress Shipping/Delivery address.
 * @property customerBillingAddress Address used for payment authorization.
 * @property deliveryDate Target date for delivery (formatted string).
 * @property orderDate Date when the order was placed (formatted string).
 * @property products Map of Product Names to Quantities.
 * @property status Current lifecycle state (PENDING, PAID, etc.).
 * @property note Optional instructions provided by the customer.
 * @property isManuallyAdded True if the order was created by an admin (e.g., phone order) rather than through the regular checkout.
 */
@Serializable
data class Order(
    val id: String,
    val customerName: String,
    val customerAddress: String,
    val customerBillingAddress: String,
    val deliveryDate: String,
    val orderDate: String,
    val products: Map<String, Int>,
    val status: OrderStatus,
    val note: String?,
    val isManuallyAdded: Boolean? = false
)