package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.model.PendingPaymentFinalization
import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference
import com.mtdevelopment.checkout.domain.repository.PaymentFinalizationScheduler

/**
 * Persists the pending-finalization marker and enqueues the durable background work.
 * Must be invoked right BEFORE submitting the payment to SumUp, so that an app kill
 * at any later point can still be reconciled.
 */
class SchedulePaymentFinalizationUseCase(
    private val checkoutDatastorePreference: CheckoutDatastorePreference,
    private val paymentFinalizationScheduler: PaymentFinalizationScheduler
) {
    suspend operator fun invoke(checkoutId: String, orderId: String) {
        checkoutDatastorePreference.setPendingFinalization(
            PendingPaymentFinalization(
                checkoutId = checkoutId,
                orderId = orderId,
                createdAtMillis = System.currentTimeMillis()
            )
        )
        paymentFinalizationScheduler.scheduleFinalizationWork()
    }
}
