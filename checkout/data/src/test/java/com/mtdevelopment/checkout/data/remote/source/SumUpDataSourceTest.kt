package com.mtdevelopment.checkout.data.remote.source

import android.util.Log
import app.cash.turbine.test
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.CHECKOUT_STATUS
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.CheckoutResponse
import com.mtdevelopment.core.util.NetWorkResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Covers the reference-based polling used by the hosted-checkout safety net
 * ([SumUpDataSource.pollHostedCheckoutStatus]): the HTTP layer is stubbed by spying on
 * [SumUpDataSource.getCheckoutsList].
 */
class SumUpDataSourceTest {

    private lateinit var dataSource: SumUpDataSource

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0

        dataSource = spyk(SumUpDataSource(mockk()))
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun checkout(
        id: String,
        status: CHECKOUT_STATUS,
        amount: Double
    ) = CheckoutResponse(
        id = id,
        status = status,
        amount = amount,
        checkoutReference = "order-1"
    )

    @Test
    fun `hosted polling emits the PAID checkout matching the expected amount`() = runTest {
        every { dataSource.getCheckoutsList("order-1") } returns flowOf(
            NetWorkResult.Success(
                listOf(
                    checkout("cheap", CHECKOUT_STATUS.PAID, amount = 0.01),
                    checkout("real", CHECKOUT_STATUS.PAID, amount = 20.5)
                )
            )
        )

        dataSource.pollHostedCheckoutStatus("order-1", expectedAmountCents = 2050L).test {
            val result = awaitItem()
            assertTrue(result is NetWorkResult.Success)
            assertEquals("real", (result as NetWorkResult.Success).data.id)
            awaitComplete()
        }
    }

    @Test
    fun `hosted polling emits FAILED when every session failed`() = runTest {
        every { dataSource.getCheckoutsList("order-1") } returns flowOf(
            NetWorkResult.Success(
                listOf(checkout("failed", CHECKOUT_STATUS.FAILED, amount = 20.5))
            )
        )

        dataSource.pollHostedCheckoutStatus("order-1", expectedAmountCents = 2050L).test {
            val result = awaitItem()
            assertTrue(result is NetWorkResult.Success)
            assertEquals(
                CHECKOUT_STATUS.FAILED,
                (result as NetWorkResult.Success).data.status
            )
            awaitComplete()
        }
    }

    @Test
    fun `hosted polling never finalizes on a PAID session with the wrong amount`() = runTest {
        // First round: only a mismatched PAID session (attacker-priced). Second round:
        // a network error, which ends the poll. The mismatched PAID must not have been
        // emitted as a success in between.
        every { dataSource.getCheckoutsList("order-1") } returnsMany listOf(
            flowOf(
                NetWorkResult.Success(
                    listOf(checkout("cheap", CHECKOUT_STATUS.PAID, amount = 0.01))
                )
            ),
            flowOf(NetWorkResult.Error("boom", "EXCEPTION"))
        )

        // One real 3 s polling delay elapses between the two rounds: raise Turbine's
        // default 3 s timeout accordingly.
        dataSource.pollHostedCheckoutStatus("order-1", expectedAmountCents = 2050L).test(
            timeout = 10.seconds
        ) {
            val result = awaitItem()
            assertTrue(result is NetWorkResult.Error)
            awaitComplete()
        }
    }

    @Test
    fun `hosted polling surfaces lookup errors`() = runTest {
        every { dataSource.getCheckoutsList("order-1") } returns flowOf(
            NetWorkResult.Error("boom", "EXCEPTION")
        )

        dataSource.pollHostedCheckoutStatus("order-1", expectedAmountCents = 2050L).test {
            val result = awaitItem()
            assertTrue(result is NetWorkResult.Error)
            assertEquals("EXCEPTION", (result as NetWorkResult.Error).code)
            awaitComplete()
        }
    }
}
