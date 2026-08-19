package com.mtdevelopment.core.model

/**
 * How the customer takes delivery of an order.
 *
 * Until this field existed, an order recorded only its delivery date and the tournée was
 * inferred from it. That inference breaks the moment a pickup and a delivery can fall on
 * the same day, which is why the mode is now recorded explicitly — most importantly so the
 * delivery-day route never loads a pickup order into the van.
 *
 * [DELIVERY] is the default everywhere: orders written before this field existed, and by
 * app versions that still do not know about it, carry no value and must read as deliveries
 * permanently — not just during a migration window.
 */
enum class FulfillmentType {
    /** Driven to the customer's address on a tournée. */
    DELIVERY,

    /** Collected by the customer at the shop. */
    PICKUP_SHOP,

    /** Collected by the customer on a market date. */
    PICKUP_MARKET;

    /** True when the order is collected rather than driven — either pickup flavour. */
    val isPickup: Boolean
        get() = this != DELIVERY

    companion object {
        /**
         * Reads a stored value, falling back to [DELIVERY] for anything unknown, absent or
         * malformed. Mirrors the forward-compatibility already applied to `OrderStatus`:
         * a value written by a newer app version must degrade, never crash the admin list.
         */
        fun fromStoredValue(value: String?): FulfillmentType =
            runCatching { valueOf(value.orEmpty()) }.getOrDefault(DELIVERY)
    }
}
