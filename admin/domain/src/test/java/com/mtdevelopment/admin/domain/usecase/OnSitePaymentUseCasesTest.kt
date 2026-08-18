package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus
import com.mtdevelopment.core.model.PaymentMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class OnSitePaymentUseCasesTest {

    private val repository: FirebaseAdminRepository = mockk()

    // A Wednesday; the grace period reaches back to 07/08/2026.
    private val today: LocalDate = LocalDate.of(2026, 8, 10)

    @Before
    fun setUp() {
        coEvery { repository.updateOrderStatus(any(), any()) } returns Result.success(Unit)
    }

    private fun order(
        id: String,
        deliveryDate: String,
        paymentMode: PaymentMode = PaymentMode.ON_SITE,
        status: OrderStatus = OrderStatus.PENDING
    ) = Order(
        id = id,
        customerName = "Jean Dupont",
        customerAddress = "",
        customerBillingAddress = "",
        deliveryDate = deliveryDate,
        orderDate = "01/08/2026",
        products = mapOf("Comté AOP" to 1),
        status = status,
        note = null,
        paymentMode = paymentMode
    )

    @Test
    fun `marking paid on site moves the order to PAID`() = runTest {
        val result = MarkOrderPaidOnSiteUseCase(repository).invoke(order("o1", "10/08/2026"))

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.updateOrderStatus("o1", OrderStatus.PAID) }
    }

    @Test
    fun `an online order cannot be marked paid by hand`() = runTest {
        // Settling one manually would paper over whatever went wrong in the payment chain.
        val result = MarkOrderPaidOnSiteUseCase(repository)
            .invoke(order("o1", "10/08/2026", paymentMode = PaymentMode.ONLINE))

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.updateOrderStatus(any(), any()) }
    }

    @Test
    fun `an unpaid on-site order older than the grace period is written off`() = runTest {
        val cancelled = CancelStaleOnSiteOrdersUseCase(repository)
            .invoke(listOf(order("o1", "05/08/2026")), today)

        assertEquals(listOf("o1"), cancelled)
        coVerify(exactly = 1) { repository.updateOrderStatus("o1", OrderStatus.CANCELED) }
    }

    @Test
    fun `an order still inside the grace period is left alone`() = runTest {
        val cancelled = CancelStaleOnSiteOrdersUseCase(repository)
            .invoke(listOf(order("o1", "08/08/2026"), order("o2", "10/08/2026")), today)

        assertTrue(cancelled.isEmpty())
        coVerify(exactly = 0) { repository.updateOrderStatus(any(), any()) }
    }

    @Test
    fun `an online order stuck in PENDING is never written off`() = runTest {
        // It may mean the customer was charged and finalization failed. Cancelling it would
        // erase the only trace of that; it must be investigated instead.
        val cancelled = CancelStaleOnSiteOrdersUseCase(repository).invoke(
            listOf(order("o1", "01/08/2026", paymentMode = PaymentMode.ONLINE)),
            today
        )

        assertTrue(cancelled.isEmpty())
        coVerify(exactly = 0) { repository.updateOrderStatus(any(), any()) }
    }

    @Test
    fun `an order already paid or cancelled is left alone`() = runTest {
        val cancelled = CancelStaleOnSiteOrdersUseCase(repository).invoke(
            listOf(
                order("o1", "01/08/2026", status = OrderStatus.PAID),
                order("o2", "01/08/2026", status = OrderStatus.CANCELED)
            ),
            today
        )

        assertTrue(cancelled.isEmpty())
    }

    @Test
    fun `an unreadable date is left alone rather than guessed at`() = runTest {
        val cancelled = CancelStaleOnSiteOrdersUseCase(repository)
            .invoke(listOf(order("o1", "2026-08-01")), today)

        assertTrue(cancelled.isEmpty())
        coVerify(exactly = 0) { repository.updateOrderStatus(any(), any()) }
    }

    @Test
    fun `a failed write is not reported as a cancellation`() = runTest {
        coEvery { repository.updateOrderStatus(any(), any()) } returns
                Result.failure(RuntimeException("offline"))

        val cancelled = CancelStaleOnSiteOrdersUseCase(repository)
            .invoke(listOf(order("o1", "01/08/2026")), today)

        assertTrue(cancelled.isEmpty())
    }
}
