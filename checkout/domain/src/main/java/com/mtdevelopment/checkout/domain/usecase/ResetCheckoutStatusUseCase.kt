package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference

class ResetCheckoutStatusUseCase(
    private val checkoutDatastorePreference: CheckoutDatastorePreference
) {
    suspend operator fun invoke() {
        checkoutDatastorePreference.resetCheckoutStatus()
    }
}