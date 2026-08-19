package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference
import com.mtdevelopment.checkout.domain.repository.OrderReminderScheduler
import kotlinx.coroutines.flow.firstOrNull

/**
 * Schedules the "your order is due today" reminder for the order that was just paid.
 *
 * Invoked on the two paths that reach PAID — the in-app one and the durable worker — so
 * a payment that completes while the app is dead still gets its reminder. Both call it
 * with the order id they just finalized: the locally saved order is only trusted when it
 * matches, otherwise a stale leftover from a previous checkout would be reminded about.
 *
 * Never invoked on order creation: an order that is never paid must not produce a
 * reminder, and abandonment leaves no event to cancel one.
 */
class ScheduleOrderReminderUseCase(
    private val checkoutDatastorePreference: CheckoutDatastorePreference,
    private val orderReminderScheduler: OrderReminderScheduler
) {
    /**
     * @param expectedOrderId The order just marked PAID.
     * @return true when a reminder was handed to the scheduler.
     */
    suspend operator fun invoke(expectedOrderId: String): Boolean {
        val order = checkoutDatastorePreference.orderFlow.firstOrNull()
            ?: return false
        if (order.id != expectedOrderId || order.deliveryDate.isBlank()) return false

        orderReminderScheduler.scheduleReminder(
            orderId = order.id,
            deliveryDate = order.deliveryDate,
            fulfillmentType = order.fulfillmentType,
            pickupLabel = order.pickupLabel,
            pickupTimeRange = order.pickupTimeRange
        )
        return true
    }
}
