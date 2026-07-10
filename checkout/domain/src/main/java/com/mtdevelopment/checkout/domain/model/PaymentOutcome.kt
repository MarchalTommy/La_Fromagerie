package com.mtdevelopment.checkout.domain.model

/**
 * Terminal outcome of a payment that was reconciled by the background
 * finalization worker while the app was **not** in the foreground — e.g. the customer
 * paid on the hosted SumUp page and the app was killed before it could return to the
 * in-app success screen.
 *
 * It is persisted by the worker at the moment it moves the order to a terminal state and
 * consumed (read once, then cleared) on the next app launch, so the customer always
 * learns the result: on [isPaid] the app routes to the success screen, otherwise it
 * surfaces a "payment not completed, cart kept" notice.
 *
 * @property orderId The finalized order id.
 * @property clientName Buyer name, used to populate the success screen.
 * @property isPaid True if the order reached PAID; false if it was CANCELED (failed).
 */
data class PaymentOutcome(
    val orderId: String,
    val clientName: String,
    val isPaid: Boolean
)
