package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.PaymentRepository
import org.json.JSONArray

class FetchAllowedPaymentMethods(
    private val paymentRepository: PaymentRepository
) {
    operator fun invoke(): JSONArray = JSONArray().put(paymentRepository.cardPaymentMethod)
}