package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference

class GetPreviouslyCreatedCheckoutUseCase(
    private val checkoutDatastorePreference: CheckoutDatastorePreference
) {
    operator fun invoke() = checkoutDatastorePreference.createdCheckoutFlow

}