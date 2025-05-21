package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.PaymentRepository
import com.mtdevelopment.core.model.OrderStatus

class UpdateOrderStatus(
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(orderId: String, newStatus: OrderStatus): Boolean {
        return paymentRepository.updateFirestoreOrderStatus(orderId, newStatus).isSuccess
    }
}