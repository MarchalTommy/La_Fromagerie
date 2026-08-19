package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.OrderReminderScheduler

/**
 * Drops the reminder of an order that will never be honoured (payment failed, order
 * canceled). Safe to call for an order that never had one.
 */
class CancelOrderReminderUseCase(
    private val orderReminderScheduler: OrderReminderScheduler
) {
    operator fun invoke(orderId: String) {
        orderReminderScheduler.cancelReminder(orderId)
    }
}
