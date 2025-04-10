package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.model.NewCheckoutResult
import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference

class SaveCreatedCheckoutUseCase(
    private val checkoutDatastorePreference: CheckoutDatastorePreference
) {
    suspend operator fun invoke(data: NewCheckoutResult) {
        checkoutDatastorePreference.saveCreatedCheckout(data)
    }
}