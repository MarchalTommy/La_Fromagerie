package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.PaymentRepository

class GetLoadPaymentDataTaskUseCase(
    private val paymentRepository: PaymentRepository
) {

    operator fun invoke(
        price: Double
    ) = paymentRepository.getLoadPaymentDataTask(price.toString())

}