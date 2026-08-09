package com.mtdevelopment.checkout.domain.repository

/**
 * Schedules the local, on-device reminder that tells the customer their order is due on
 * the current day.
 *
 * Deliberately local rather than pushed from the server: the app's FCM targeting is
 * topic-based (every client listens to the same topic, no per-device token registry), so
 * a per-customer reminder cannot go through it. The delivery date is known at checkout on
 * the very device that placed the order, which is all the information a reminder needs.
 *
 * Implementations are flavor-specific: the client posts a real notification, the admin
 * flavor does nothing (it shares the checkout screen but has no customer to remind).
 */
interface OrderReminderScheduler {

    /**
     * Schedules the reminder for [orderId], due on [deliveryDate] (`dd/MM/yyyy`).
     *
     * Scheduling the same [orderId] twice replaces the previous reminder rather than
     * adding a second one. A date that resolves to an instant already in the past is
     * skipped: a reminder for a day that is over would only confuse.
     */
    fun scheduleReminder(orderId: String, deliveryDate: String)

    /** Drops a previously scheduled reminder; a no-op when none exists. */
    fun cancelReminder(orderId: String)
}
