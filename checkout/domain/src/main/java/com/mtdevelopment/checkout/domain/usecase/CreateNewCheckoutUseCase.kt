package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.PaymentRepository

class CreateNewCheckoutUseCase(
    private val paymentRepository: PaymentRepository,
) {
    operator fun invoke(
        amount: Double,
        description: String,
        buyerName: String,
        buyerAddress: String,
        buyerEmail: String,
        reference: String
    ) =
        paymentRepository.createNewCheckout(
            amount,
            description,
            buyerName,
            buyerAddress,
            buyerEmail,
            reference
        )
}