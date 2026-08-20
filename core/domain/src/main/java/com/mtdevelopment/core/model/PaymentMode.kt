package com.mtdevelopment.core.model

/**
 * How an order is paid for.
 *
 * Exists to disambiguate `OrderStatus.PENDING`, which otherwise carries two incompatible
 * meanings once payment on collection is allowed: "payment in flight" and "to be paid on
 * the spot". The distinction is load-bearing — an [ONLINE] order stuck in PENDING signals
 * a payment that may have taken the customer's money and needs investigating, while an
 * [ON_SITE] one is simply waiting for the customer to turn up.
 *
 * A separate field rather than a new `OrderStatus` value on purpose: status readers
 * default anything unknown to PENDING, so an older app version would silently render a
 * new status as exactly the state we are trying to tell apart. An unknown *field* is
 * merely ignored.
 */
enum class PaymentMode {
    /** Paid in the app through Google Pay / SumUp before the order is honoured. */
    ONLINE,

    /**
     * Paid to the shop when the order is collected.
     *
     * Only ever written on a [FulfillmentType.PICKUP_SHOP] order, by the shop's decision of
     * 2026-08-20: money is taken over the counter, not on a market stall. `CheckoutViewModel`
     * is the single writer and holds the check; nothing here enforces it, because a reader —
     * the admin app above all — must still be able to make sense of any ON_SITE order it
     * finds, whatever its collection mode.
     */
    ON_SITE;

    companion object {
        /** Reads a stored value, falling back to [ONLINE] for anything unknown or absent. */
        fun fromStoredValue(value: String?): PaymentMode =
            runCatching { valueOf(value.orEmpty()) }.getOrDefault(ONLINE)
    }
}
