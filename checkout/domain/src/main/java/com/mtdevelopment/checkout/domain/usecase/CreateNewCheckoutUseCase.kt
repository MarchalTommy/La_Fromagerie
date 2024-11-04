package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.PaymentRepository

class CreateNewCheckoutUseCase(
    private val paymentRepository: PaymentRepository,
) {
    operator fun invoke(amount: Double, reference: String) =
        paymentRepository.createNewCheckout(amount, reference)
}