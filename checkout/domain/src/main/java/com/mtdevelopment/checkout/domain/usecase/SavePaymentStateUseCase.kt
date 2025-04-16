package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference

class SavePaymentStateUseCase(
    private val checkoutDatastorePreference: CheckoutDatastorePreference
) {
    suspend operator fun invoke(isSuccess: Boolean) {
        checkoutDatastorePreference.setIsCheckoutSuccessful(isSuccess)
    }
}