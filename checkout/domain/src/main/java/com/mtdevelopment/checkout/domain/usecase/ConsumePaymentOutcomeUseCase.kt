package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.model.PaymentOutcome
import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference
import kotlinx.coroutines.flow.first

/**
 * Reads the pending [PaymentOutcome] — recorded by the background finalization worker
 * when it reconciled a payment while the app was away — and clears it in the same call,
 * so the result is surfaced to the customer exactly once on the next launch.
 *
 * Returns null when there is nothing to surface (the common case: the payment was
 * finalized in-app and the customer already saw the result).
 */
class ConsumePaymentOutcomeUseCase(
    private val checkoutDatastorePreference: CheckoutDatastorePreference
) {
    suspend operator fun invoke(): PaymentOutcome? {
        val outcome = checkoutDatastorePreference.paymentOutcomeFlow.first()
        if (outcome != null) {
            checkoutDatastorePreference.clearPaymentOutcome()
        }
        return outcome
    }
}
