package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.PaymentRepository

class FetchAllowedPaymentMethods(
    private val paymentRepository: PaymentRepository
) {
    operator fun invoke() = paymentRepository.cardPaymentMethod
}