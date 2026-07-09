package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.model.PendingPaymentFinalization
import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference
import com.mtdevelopment.checkout.domain.repository.PaymentFinalizationScheduler

/**
 * Persists the pending-finalization marker and enqueues the durable background work.
 * Must be invoked right BEFORE submitting the payment to SumUp (Google Pay path) or
 * BEFORE opening the hosted-checkout page (hosted path), so that an app kill at any
 * later point can still be reconciled.
 *
 * @param checkoutId The SumUp session id, or null on the hosted path where only the
 * payment URL is known; the worker then resolves the session by reference (= [orderId]).
 * @param expectedAmountCents Required with a null [checkoutId]: a reference-resolved
 * PAID session is only trusted if its amount matches the order total.
 */
class SchedulePaymentFinalizationUseCase(
    private val checkoutDatastorePreference: CheckoutDatastorePreference,
    private val paymentFinalizationScheduler: PaymentFinalizationScheduler
) {
    suspend operator fun invoke(
        checkoutId: String?,
        orderId: String,
        expectedAmountCents: Long? = null
    ) {
        checkoutDatastorePreference.setPendingFinalization(
            PendingPaymentFinalization(
                checkoutId = checkoutId,
                orderId = orderId,
                createdAtMillis = System.currentTimeMillis(),
                expectedAmountCents = expectedAmountCents
            )
        )
        paymentFinalizationScheduler.scheduleFinalizationWork()
    }
}
