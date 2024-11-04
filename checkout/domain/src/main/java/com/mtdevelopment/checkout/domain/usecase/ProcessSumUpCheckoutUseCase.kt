package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.model.GooglePayData
import com.mtdevelopment.checkout.domain.repository.PaymentRepository

class ProcessSumUpCheckoutUseCase(
    private val paymentRepository: PaymentRepository
) {
    operator fun invoke(reference: String, googlePayData: GooglePayData) =
        paymentRepository.processCheckout(reference, googlePayData)
}