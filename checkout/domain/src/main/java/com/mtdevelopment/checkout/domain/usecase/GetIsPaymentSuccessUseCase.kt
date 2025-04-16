package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference

class GetIsPaymentSuccessUseCase(
    private val checkoutDatastorePreference: CheckoutDatastorePreference
) {
    operator fun invoke() = checkoutDatastorePreference.isCheckoutSuccessfulFlow
}