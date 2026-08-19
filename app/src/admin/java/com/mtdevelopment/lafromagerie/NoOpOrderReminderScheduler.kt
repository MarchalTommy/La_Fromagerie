package com.mtdevelopment.lafromagerie

import com.mtdevelopment.checkout.domain.repository.OrderReminderScheduler
import com.mtdevelopment.core.model.FulfillmentType

/**
 * Admin-flavor binding for [OrderReminderScheduler].
 *
 * The admin flavor reaches the checkout screen too, so `CheckoutViewModel` — and through
 * it `ScheduleOrderReminderUseCase` — is instantiated here as well. Koin resolves
 * definitions at runtime, so leaving this unbound would not fail the build: it would
 * crash the shop owner's app the first time he opened checkout.
 *
 * Doing nothing is the correct behaviour, not a stub: an order the owner puts through
 * himself has no customer device to remind.
 */
class NoOpOrderReminderScheduler : OrderReminderScheduler {

    override fun scheduleReminder(
        orderId: String,
        deliveryDate: String,
        fulfillmentType: FulfillmentType,
        pickupLabel: String?,
        pickupTimeRange: String?
    ) = Unit

    override fun cancelReminder(orderId: String) = Unit
}
