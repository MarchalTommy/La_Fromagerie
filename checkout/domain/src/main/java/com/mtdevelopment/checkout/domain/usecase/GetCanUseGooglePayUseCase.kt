package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.PaymentRepository

class GetCanUseGooglePayUseCase(
    private val paymentRepository: PaymentRepository
) {

    suspend operator fun invoke(): Boolean? {
        return paymentRepository.canUseGooglePay()
    }

}