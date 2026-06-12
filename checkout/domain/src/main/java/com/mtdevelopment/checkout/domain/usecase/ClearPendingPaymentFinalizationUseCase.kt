package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference

/**
 * Clears the pending-finalization marker once the order has reached a terminal state
 * through the in-app flow, so the background work becomes a no-op.
 */
class ClearPendingPaymentFinalizationUseCase(
    private val checkoutDatastorePreference: CheckoutDatastorePreference
) {
    suspend operator fun invoke() {
        checkoutDatastorePreference.clearPendingFinalization()
    }
}
