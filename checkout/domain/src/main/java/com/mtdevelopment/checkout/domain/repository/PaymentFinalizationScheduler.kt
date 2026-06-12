package com.mtdevelopment.checkout.domain.repository

/**
 * Schedules the background work that brings a submitted payment to its terminal state
 * (polling SumUp, updating the Firestore order, clearing the cart) in a way that
 * survives the app process being killed.
 */
interface PaymentFinalizationScheduler {

    /**
     * Enqueues (or re-enqueues) the finalization work for the currently pending payment.
     * The work reads its input from [CheckoutDatastorePreference.pendingFinalizationFlow].
     */
    fun scheduleFinalizationWork()
}
