package com.mtdevelopment.core.model

import kotlinx.serialization.Serializable

/**
 * Core domain model representing a customer order.
 * 
 * @property id Unique identifier (generated during checkout).
 * @property customerName Full name of the buyer.
 * @property customerEmail Contact email captured at checkout, used to send the purchase
 *   confirmation. Null on documents written before this field existed (and on some
 *   admin-created orders).
 * @property customerAddress Shipping/Delivery address.
 * @property customerBillingAddress Address used for payment authorization.
 * @property deliveryDate Target date for delivery (formatted string).
 * @property orderDate Date when the order was placed (formatted string).
 * @property products Map of Product Names to Quantities.
 * @property status Current lifecycle state (PENDING, PAID, etc.).
 * @property note Optional instructions provided by the customer.
 * @property isManuallyAdded True if the order was created by an admin (e.g., phone order) rather than through the regular checkout.
 * @property totalPrice Total order amount in cents. Null on documents written before this field existed.
 * @property fulfillmentType How the order reaches the customer. Defaults to
 *   [FulfillmentType.DELIVERY], which is also what every order written before this field
 *   existed reads as — permanently, since older app versions keep writing without it.
 * @property paymentMode Whether the order was paid in the app or is to be paid on
 *   collection. Defaults to [PaymentMode.ONLINE].
 * @property customerPhone Contact number, collected for pickup orders where there is no
 *   address to fall back on if the customer does not turn up. Null on deliveries.
 * @property pickupPointId Identifier of the shop or market date the order is collected at.
 *   Null on deliveries.
 * @property pickupLabel Human-readable name of the pickup point, e.g. "Marché de
 *   Pontarlier". **Snapshot, not a reference**: copied here at order time so editing or
 *   deleting the pickup point later cannot rewrite past orders.
 * @property pickupAddress Address of the pickup point, snapshotted like [pickupLabel].
 * @property pickupTimeRange Opening window displayed to the customer, e.g. "8h-13h",
 *   snapshotted like [pickupLabel].
 */
@Serializable
data class Order(
    val id: String,
    val customerName: String,
    val customerEmail: String? = null,
    val customerAddress: String,
    val customerBillingAddress: String,
    val deliveryDate: String,
    val orderDate: String,
    val products: Map<String, Int>,
    val status: OrderStatus,
    val note: String?,
    val isManuallyAdded: Boolean? = false,
    val totalPrice: Long? = null,
    val fulfillmentType: FulfillmentType = FulfillmentType.DELIVERY,
    val paymentMode: PaymentMode = PaymentMode.ONLINE,
    val customerPhone: String? = null,
    val pickupPointId: String? = null,
    val pickupLabel: String? = null,
    val pickupAddress: String? = null,
    val pickupTimeRange: String? = null
)