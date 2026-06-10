package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference
import com.mtdevelopment.checkout.domain.repository.PaymentFinalizationScheduler
import kotlinx.coroutines.flow.first

/**
 * Called at app launch: if a payment was submitted but never reached a terminal state
 * (the app was killed mid-processing), re-enqueue the finalization work.
 * Markers older than [PendingPaymentFinalization.MAX_AGE_MILLIS] are dropped instead,
 * as the matching SumUp checkout session has expired by then.
 */
class ResumePendingPaymentFinalizationUseCase(
    private val checkoutDatastorePreference: CheckoutDatastorePreference,
    private val paymentFinalizationScheduler: PaymentFinalizationScheduler
) {
    suspend operator fun invoke() {
        val pending = checkoutDatastorePreference.pendingFinalizationFlow.first() ?: return
        if (pending.isExpired(System.currentTimeMillis())) {
            checkoutDatastorePreference.clearPendingFinalization()
        } else {
            paymentFinalizationScheduler.scheduleFinalizationWork()
        }
    }
}
