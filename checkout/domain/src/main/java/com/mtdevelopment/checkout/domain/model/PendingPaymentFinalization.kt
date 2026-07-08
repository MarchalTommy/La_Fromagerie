package com.mtdevelopment.checkout.domain.model

/**
 * Marker persisted while a payment is being processed by SumUp.
 *
 * It is written right before the payment is submitted and cleared once the matching
 * order has reached a terminal state (PAID or FAILED) in Firestore. If the app is
 * killed in between, this marker allows the finalization work to resume and reconcile
 * the order status on the next launch.
 *
 * Two shapes exist, one per payment path:
 * - Google Pay path: [checkoutId] is known at submission time and the worker polls it
 *   directly.
 * - Hosted-checkout path: the Cloud Function only returns the payment URL, so
 *   [checkoutId] is null. The worker resolves the SumUp session through its
 *   `checkout_reference`, which equals [orderId] on that path, and only accepts a PAID
 *   session whose amount matches [expectedAmountCents].
 *
 * @property checkoutId The SumUp checkout session id to poll, or null when unknown
 * (hosted checkout).
 * @property orderId The Firestore order id whose status must be updated. On the hosted
 * path it is also the SumUp `checkout_reference`.
 * @property createdAtMillis When the payment was submitted; used to expire stale markers.
 * @property expectedAmountCents Order total used to validate a reference-resolved PAID
 * session; null on the Google Pay path where the session id is trusted directly.
 */
data class PendingPaymentFinalization(
    val checkoutId: String?,
    val orderId: String,
    val createdAtMillis: Long,
    val expectedAmountCents: Long? = null
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
