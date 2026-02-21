package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.model.GooglePayData
import com.mtdevelopment.checkout.domain.model.ProcessCheckoutResult
import com.mtdevelopment.checkout.domain.repository.PaymentRepository

class ProcessSumUpCheckoutUseCase(
    private val paymentRepository: PaymentRepository
) {
    operator fun invoke(
        checkoutId: String,
        googlePayData: GooglePayData,
        on3DSecureRequired: (ProcessCheckoutResult.NextStep) -> Unit
    ) =
        paymentRepository.processCheckout(checkoutId, googlePayData, on3DSecureRequired)
}