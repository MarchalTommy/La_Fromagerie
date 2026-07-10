package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.model.PendingPaymentFinalization
import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference
import com.mtdevelopment.checkout.domain.repository.PaymentFinalizationScheduler
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PaymentFinalizationUseCasesTest {

    private val preferences: CheckoutDatastorePreference = mockk()
    private val scheduler: PaymentFinalizationScheduler = mockk()

    @Before
    fun setUp() {
        coEvery { preferences.setPendingFinalization(any()) } just Runs
        coEvery { preferences.clearPendingFinalization() } just Runs
        coEvery { scheduler.scheduleFinalizationWork() } just Runs
    }

    @Test
    fun `schedule persists marker before enqueueing work`() = runTest {
        val markerSlot = slot<PendingPaymentFinalization>()
        coEvery { preferences.setPendingFinalization(capture(markerSlot)) } just Runs

        SchedulePaymentFinalizationUseCase(preferences, scheduler)
            .invoke(checkoutId = "checkout-1", orderId = "order-1")

        assertEquals("checkout-1", markerSlot.captured.checkoutId)
        assertEquals("order-1", markerSlot.captured.orderId)
        assertNull(markerSlot.captured.expectedAmountCents)
        coVerify(exactly = 1) { preferences.setPendingFinalization(any()) }
        verify(exactly = 1) { scheduler.scheduleFinalizationWork() }
    }

    @Test
    fun `schedule persists a hosted marker without checkout id but with expected amount`() =
        runTest {
            val markerSlot = slot<PendingPaymentFinalization>()
            coEvery { preferences.setPendingFinalization(capture(markerSlot)) } just Runs

            SchedulePaymentFinalizationUseCase(preferences, scheduler)
                .invoke(checkoutId = null, orderId = "order-1", expectedAmountCents = 2050L)

            assertNull(markerSlot.captured.checkoutId)
            assertEquals("order-1", markerSlot.captured.orderId)
            assertEquals(2050L, markerSlot.captured.expectedAmountCents)
            verify(exactly = 1) { scheduler.scheduleFinalizationWork() }
        }

    @Test
    fun `resume does nothing when no marker is present`() = runTest {
        coEvery { preferences.pendingFinalizationFlow } returns flowOf(null)

        ResumePendingPaymentFinalizationUseCase(preferences, scheduler).invoke()

        verify(exactly = 0) { scheduler.scheduleFinalizationWork() }
        coVerify(exactly = 0) { preferences.clearPendingFinalization() }
    }

    @Test
    fun `resume re-schedules work for a fresh marker`() = runTest {
        coEvery { preferences.pendingFinalizationFlow } returns flowOf(
            PendingPaymentFinalization(
                checkoutId = "checkout-1",
                orderId = "order-1",
                createdAtMillis = System.currentTimeMillis()
            )
        )

        ResumePendingPaymentFinalizationUseCase(preferences, scheduler).invoke()

        verify(exactly = 1) { scheduler.scheduleFinalizationWork() }
        coVerify(exactly = 0) { preferences.clearPendingFinalization() }
    }

    @Test
    fun `resume drops an expired marker instead of scheduling`() = runTest {
        coEvery { preferences.pendingFinalizationFlow } returns flowOf(
            PendingPaymentFinalization(
                checkoutId = "checkout-1",
                orderId = "order-1",
                createdAtMillis = System.currentTimeMillis() -
                        PendingPaymentFinalization.MAX_AGE_MILLIS - 1
            )
        )

        ResumePendingPaymentFinalizationUseCase(preferences, scheduler).invoke()

        verify(exactly = 0) { scheduler.scheduleFinalizationWork() }
        coVerify(exactly = 1) { preferences.clearPendingFinalization() }
    }

    @Test
    fun `clear use case clears the marker`() = runTest {
        ClearPendingPaymentFinalizationUseCase(preferences).invoke()

        coVerify(exactly = 1) { preferences.clearPendingFinalization() }
    }

    @Test
    fun `marker expiry is based on creation age`() {
        val marker = PendingPaymentFinalization("c", "o", createdAtMillis = 1_000L)

        assertFalse(marker.isExpired(nowMillis = 1_000L + PendingPaymentFinalization.MAX_AGE_MILLIS))
        assertTrue(marker.isExpired(nowMillis = 1_001L + PendingPaymentFinalization.MAX_AGE_MILLIS))
    }
}
