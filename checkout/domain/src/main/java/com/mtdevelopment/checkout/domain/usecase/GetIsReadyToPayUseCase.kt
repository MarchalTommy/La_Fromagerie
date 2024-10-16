package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.PaymentRepository

class GetIsReadyToPayUseCase(
    private val paymentRepository: PaymentRepository,
) {

    suspend operator fun invoke() = paymentRepository.isReadyToPayRequest()

}