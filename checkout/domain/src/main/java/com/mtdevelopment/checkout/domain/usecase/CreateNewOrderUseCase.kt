package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference
import com.mtdevelopment.checkout.domain.repository.PaymentRepository
import com.mtdevelopment.core.model.Order

class CreateNewOrderUseCase(
    private val paymentRepository: PaymentRepository,
    private val checkoutDatastorePreference: CheckoutDatastorePreference
) {
    suspend operator fun invoke(order: Order): Boolean {
        checkoutDatastorePreference.saveOrder(order)
        return paymentRepository.createFirestoreOrder(order).isSuccess
    }
}