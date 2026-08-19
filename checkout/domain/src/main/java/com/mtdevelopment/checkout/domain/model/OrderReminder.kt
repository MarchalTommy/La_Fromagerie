package com.mtdevelopment.checkout.domain.model

import com.mtdevelopment.core.domain.toLocalDate
import java.time.ZoneId

/**
 * Timing rule for the order reminder: when, on the delivery day, the customer is nudged.
 *
 * Lives in the domain rather than in the scheduler implementation so the rule is unit
 * tested without WorkManager or an Android context.
 */
object OrderReminder {

    /**
     * Local hour of the delivery day at which the reminder fires. Early enough to be seen
     * before a morning tournée, late enough not to wake anyone.
     */
    const val REMINDER_HOUR: Int = 8

    /**
     * Resolves the instant at which the reminder for [deliveryDate] (`dd/MM/yyyy`) should
     * fire, or null when it should not be scheduled at all.
     *
     * Null is returned for an unparseable date and for an instant already past — the two
     * cases where firing would be worse than staying silent. The zone is the device's:
     * "8am on delivery day" only means anything in the customer's own time.
     *
     * @param nowMillis Current time, injected so the rule is testable.
     * @param zone Device time zone, injected for the same reason.
     */
    fun reminderTimeMillis(
        deliveryDate: String,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long? {
        val date = deliveryDate.toLocalDate() ?: return null
        val reminderMillis = date.atTime(REMINDER_HOUR, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        return reminderMillis.takeIf { it > nowMillis }
    }
}
