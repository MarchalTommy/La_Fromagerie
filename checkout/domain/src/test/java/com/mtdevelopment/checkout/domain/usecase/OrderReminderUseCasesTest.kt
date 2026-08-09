package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference
import com.mtdevelopment.checkout.domain.repository.OrderReminderScheduler
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OrderReminderUseCasesTest {

    private val preferences: CheckoutDatastorePreference = mockk()
    private val scheduler: OrderReminderScheduler = mockk()

    @Before
    fun setUp() {
        every { scheduler.scheduleReminder(any(), any()) } just Runs
        every { scheduler.cancelReminder(any()) } just Runs
    }

    private fun order(id: String = "order-1", deliveryDate: String = "15/08/2026") = Order(
        id = id,
        customerName = "Jean Dupont",
        customerAddress = "1 rue du Comté, 25300 Pontarlier",
        customerBillingAddress = "1 rue du Comté, 25300 Pontarlier",
        deliveryDate = deliveryDate,
        orderDate = "10/08/2026",
        products = mapOf("Comté AOP" to 2),
        status = OrderStatus.PAID,
        note = null
    )

    @Test
    fun `schedules the reminder for the order that was just paid`() = runTest {
        coEvery { preferences.orderFlow } returns flowOf(order())

        val scheduled = ScheduleOrderReminderUseCase(preferences, scheduler).invoke("order-1")

        assertTrue(scheduled)
        verify(exactly = 1) { scheduler.scheduleReminder("order-1", "15/08/2026") }
    }

    @Test
    fun `schedules nothing when no order is saved locally`() = runTest {
        coEvery { preferences.orderFlow } returns flowOf(null)

        val scheduled = ScheduleOrderReminderUseCase(preferences, scheduler).invoke("order-1")

        assertFalse(scheduled)
        verify(exactly = 0) { scheduler.scheduleReminder(any(), any()) }
    }

    @Test
    fun `schedules nothing when the saved order is a leftover from another checkout`() = runTest {
        // Guards against reminding the customer about a stale order still sitting in the
        // local datastore instead of the one that was actually just paid.
        coEvery { preferences.orderFlow } returns flowOf(order(id = "order-previous"))

        val scheduled = ScheduleOrderReminderUseCase(preferences, scheduler).invoke("order-1")

        assertFalse(scheduled)
        verify(exactly = 0) { scheduler.scheduleReminder(any(), any()) }
    }

    @Test
    fun `schedules nothing when the order carries no delivery date`() = runTest {
        coEvery { preferences.orderFlow } returns flowOf(order(deliveryDate = ""))

        val scheduled = ScheduleOrderReminderUseCase(preferences, scheduler).invoke("order-1")

        assertFalse(scheduled)
        verify(exactly = 0) { scheduler.scheduleReminder(any(), any()) }
    }

    @Test
    fun `cancel delegates to the scheduler`() {
        CancelOrderReminderUseCase(scheduler).invoke("order-1")

        verify(exactly = 1) { scheduler.cancelReminder("order-1") }
    }
}
