package com.mtdevelopment.checkout.domain.model

/**
 * Marker persisted while a payment is being processed by SumUp.
 *
 * It is written right before the payment is submitted and cleared once the matching
 * order has reached a terminal state (PAID or FAILED) in Firestore. If the app is
 * killed in between, this marker allows the finalization work to resume and reconcile
 * the order status on the next launch.
 *
 * @property checkoutId The SumUp checkout session id to poll.
 * @property orderId The Firestore order id whose status must be updated.
 * @property createdAtMillis When the payment was submitted; used to expire stale markers.
 */
data class PendingPaymentFinalization(
    val checkoutId: String,
    val orderId: String,
    val createdAtMillis: Long
) {
    fun isExpired(nowMillis: Long): Boolean =
        nowMillis - createdAtMillis > MAX_AGE_MILLIS

    companion object {
        /**
         * After this delay we stop trying to reconcile automatically: the SumUp
         * dashboard remains the source of truth for the money side.
         */
        const val MAX_AGE_MILLIS: Long = 24L * 60L * 60L * 1000L
    }
}
