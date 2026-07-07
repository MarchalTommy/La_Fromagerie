package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.model.Checkout
import com.mtdevelopment.checkout.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow

class GetCheckoutsByReferenceUseCase(
    private val paymentRepository: PaymentRepository
) {
    operator fun invoke(reference: String): Flow<List<Checkout>> {
        return paymentRepository.getCheckoutsByReference(reference)
    }
}
