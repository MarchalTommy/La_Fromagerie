package com.mtdevelopment.core.model

/**
 * One batch the shop prepares as a unit: everything due on a given date **for a given
 * destination**.
 *
 * The date alone is not enough. A single day can carry a tournée, orders collected at the
 * shop and orders collected on a market — three batches that leave the workshop separately.
 * Aggregating them by date would produce one "4 Comté" with no way to tell what goes in
 * the van, which is exactly the mistake this type exists to prevent.
 *
 * @property deliveryDate The `dd/MM/yyyy` date the batch is due.
 * @property fulfillmentType How that batch leaves the shop.
 * @property pickupPointId Which pickup point, when [fulfillmentType] is a pickup; null for
 *   deliveries. Two markets on the same day are therefore two distinct batches.
 * @property pickupLabel Human-readable name of the pickup point, for display; null for
 *   deliveries, whose header is the date alone.
 */
data class PreparationGroup(
    val deliveryDate: String,
    val fulfillmentType: FulfillmentType = FulfillmentType.DELIVERY,
    val pickupPointId: String? = null,
    val pickupLabel: String? = null
) {

    /**
     * Identifier of the "product prepared" tick for [productName] within this batch.
     *
     * Deliveries keep the historical `ddMMyyyy_ProductName` form so the preparation ticks
     * already stored in Firestore keep matching — this evolution costs the shop no lost
     * state. Pickups, which never had such documents, get the point appended so two
     * batches of the same product on the same day cannot collide.
     */
    fun statusIdFor(productName: String): String {
        val base = "${deliveryDate.replace("/", "")}_${productName.replace(" ", "")}"
        return if (fulfillmentType == FulfillmentType.DELIVERY) {
            base
        } else {
            "${base}_${pickupPointId.orEmpty().replace(" ", "")}"
        }
    }
}

/** The batch this order belongs to. */
val Order.preparationGroup: PreparationGroup
    get() = PreparationGroup(
        deliveryDate = deliveryDate,
        fulfillmentType = fulfillmentType,
        pickupPointId = pickupPointId,
        pickupLabel = pickupLabel
    )

/**
 * Keeps only the orders the shop has to drive to on a tournée.
 *
 * Used by every consumer of the delivery day — the route optimizer, the maps link, the
 * tracking service. Without it a click & collect order becomes a stop at the customer's
 * home address, which is why this filter is a correctness requirement and not a nicety.
 */
fun List<Order>.deliveriesOnly(): List<Order> =
    filter { it.fulfillmentType == FulfillmentType.DELIVERY }
