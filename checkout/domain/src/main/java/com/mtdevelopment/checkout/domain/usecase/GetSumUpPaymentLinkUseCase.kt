package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.PaymentRepository

class GetSumUpPaymentLinkUseCase(
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(amount: Double, orderId: String): Result<String> {
        return paymentRepository.getSumUpPaymentLink(amount, orderId)
    }
}
