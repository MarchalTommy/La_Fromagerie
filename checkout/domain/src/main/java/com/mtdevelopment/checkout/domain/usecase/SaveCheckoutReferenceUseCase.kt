package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference

class SaveCheckoutReferenceUseCase(
    private val checkoutDatastorePreference: CheckoutDatastorePreference
) {
    suspend operator fun invoke(reference: String) {
        checkoutDatastorePreference.saveCheckoutReference(reference)
    }

}