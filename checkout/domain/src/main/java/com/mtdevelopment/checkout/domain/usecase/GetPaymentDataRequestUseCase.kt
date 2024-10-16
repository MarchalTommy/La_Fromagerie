package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.PaymentRepository

class GetPaymentDataRequestUseCase(
    private val paymentRepository: PaymentRepository
) {

    operator fun invoke(
        price: Long
    ) = paymentRepository.getPaymentDataRequest(price)

}