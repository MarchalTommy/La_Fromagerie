package com.mtdevelopment.checkout.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class OrderReminderTest {

    private val paris: ZoneId = ZoneId.of("Europe/Paris")

    private fun millisAt(date: String, hour: Int, minute: Int = 0): Long {
        val (day, month, year) = date.split("/").map { it.toInt() }
        return LocalDate.of(year, month, day)
            .atTime(LocalTime.of(hour, minute))
            .atZone(paris)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun `reminder fires at 8am local time on the delivery day`() {
        val actual = OrderReminder.reminderTimeMillis(
            deliveryDate = "15/08/2026",
            nowMillis = millisAt("10/08/2026", hour = 12),
            zone = paris
        )

        assertEquals(millisAt("15/08/2026", hour = OrderReminder.REMINDER_HOUR), actual)
    }

    @Test
    fun `a delivery day already over is not scheduled`() {
        val actual = OrderReminder.reminderTimeMillis(
            deliveryDate = "01/08/2026",
            nowMillis = millisAt("10/08/2026", hour = 12),
            zone = paris
        )

        assertNull(actual)
    }

    @Test
    fun `today past the reminder hour is not scheduled`() {
        // Reminding someone at noon that their order arrives "today at 8am" is noise:
        // the slot is gone, so nothing is enqueued.
        val actual = OrderReminder.reminderTimeMillis(
            deliveryDate = "10/08/2026",
            nowMillis = millisAt("10/08/2026", hour = 12),
            zone = paris
        )

        assertNull(actual)
    }

    @Test
    fun `today before the reminder hour is still scheduled`() {
        val actual = OrderReminder.reminderTimeMillis(
            deliveryDate = "10/08/2026",
            nowMillis = millisAt("10/08/2026", hour = 6),
            zone = paris
        )

        assertEquals(millisAt("10/08/2026", hour = OrderReminder.REMINDER_HOUR), actual)
    }

    @Test
    fun `an unparseable date is not scheduled`() {
        val actual = OrderReminder.reminderTimeMillis(
            deliveryDate = "2026-08-15",
            nowMillis = millisAt("10/08/2026", hour = 12),
            zone = paris
        )

        assertNull(actual)
    }

    @Test
    fun `a blank date is not scheduled`() {
        val actual = OrderReminder.reminderTimeMillis(
            deliveryDate = "",
            nowMillis = millisAt("10/08/2026", hour = 12),
            zone = paris
        )

        assertNull(actual)
    }
}
