package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.model.GooglePayData
import com.mtdevelopment.checkout.domain.repository.PaymentRepository

class ProcessSumUpCheckoutUseCase(
    private val paymentRepository: PaymentRepository
) {
    operator fun invoke(
        checkoutId: String,
        googlePayData: GooglePayData,
        is3DSecure: (String?) -> Unit
    ) =
        paymentRepository.processCheckout(checkoutId, googlePayData, is3DSecure)
}