package com.mtdevelopment.checkout.data.repository

import android.content.Context
import android.util.Log
import app.cash.turbine.test
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.CHECKOUT_STATUS
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.CheckoutResponse
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.NewCheckoutResponse
import com.mtdevelopment.checkout.data.remote.source.FirestoreOrderDataSource
import com.mtdevelopment.checkout.data.remote.source.SumUpDataSource
import com.mtdevelopment.checkout.domain.model.GooglePayData
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus
import com.mtdevelopment.core.util.NetWorkResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PaymentRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var sumUpDataSource: SumUpDataSource
    private lateinit var firestoreOrderDataSource: FirestoreOrderDataSource
    private lateinit var repository: PaymentRepositoryImpl

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0

        context = mockk(relaxed = true)
        sumUpDataSource = mockk()
        firestoreOrderDataSource = mockk()
        repository = PaymentRepositoryImpl(context, sumUpDataSource, firestoreOrderDataSource)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    // region createNewCheckout

    @Test
    fun `createNewCheckout emits mapped result on success`() = runTest {
        coEvery { sumUpDataSource.createNewCheckout(any()) } returns flowOf(
            NetWorkResult.Success(
                NewCheckoutResponse(
                    id = "checkout-1",
                    status = "PENDING",
                    amount = 10.5,
                    checkoutReference = "ref-1"
                )
            )
        )

        repository.createNewCheckout(
            amount = 10.5,
            description = "2 x Comté",
            buyerName = "Jane",
            buyerAddress = "1 rue du Fromage",
            buyerEmail = "jane@example.com",
            reference = "ref-1"
        ).test {
            val result = awaitItem()
            assertEquals("checkout-1", result.id)
            assertEquals("PENDING", result.status)
            assertEquals("ref-1", result.checkoutReference)
            awaitComplete()
        }
    }

    @Test
    fun `createNewCheckout emits null status marker on network error`() = runTest {
        coEvery { sumUpDataSource.createNewCheckout(any()) } returns flowOf(
            NetWorkResult.Error("timeout", "EXCEPTION")
        )

        repository.createNewCheckout(
            amount = 10.5,
            description = "",
            buyerName = "Jane",
            buyerAddress = "",
            buyerEmail = "",
            reference = "ref-1"
        ).test {
            val result = awaitItem()
            assertNull(result.status)
            assertNull(result.id)
            awaitComplete()
        }
    }

    @Test
    fun `createNewCheckout emits null status marker when body is empty`() = runTest {
        coEvery { sumUpDataSource.createNewCheckout(any()) } returns flowOf(
            NetWorkResult.Success(null)
        )

        repository.createNewCheckout(
            amount = 1.0,
            description = "",
            buyerName = "Jane",
            buyerAddress = "",
            buyerEmail = "",
            reference = "ref-1"
        ).test {
            assertNull(awaitItem().status)
            awaitComplete()
        }
    }

    // endregion

    // region processCheckout

    private val googlePayData = GooglePayData(
        apiVersion = 2,
        apiVersionMinor = 0,
        paymentMethodData = null
    )

    @Test
    fun `processCheckout emits domain checkout on terminal PAID status`() = runTest {
        coEvery { sumUpDataSource.processCheckout(any(), any()) } returns flowOf(
            NetWorkResult.Success(
                CheckoutResponse(
                    id = "checkout-1",
                    status = CHECKOUT_STATUS.PAID,
                    amount = 10.5,
                    checkoutReference = "ref-1",
                    currency = "EUR",
                    merchantCode = "MERCHANT"
                )
            )
        )

        repository.processCheckout("checkout-1", googlePayData) {}.test {
            val checkout = awaitItem()
            assertEquals("checkout-1", checkout.id)
            assertEquals("PAID", checkout.status?.value)
            awaitComplete()
        }
    }

    @Test
    fun `processCheckout filters out non terminal statuses`() = runTest {
        coEvery { sumUpDataSource.processCheckout(any(), any()) } returns flowOf(
            NetWorkResult.Success(
                CheckoutResponse(id = "checkout-1", status = CHECKOUT_STATUS.PENDING)
            )
        )

        repository.processCheckout("checkout-1", googlePayData) {}.test {
            awaitComplete()
        }
    }

    @Test
    fun `processCheckout surfaces errors as flow exception`() = runTest {
        coEvery { sumUpDataSource.processCheckout(any(), any()) } returns flowOf(
            NetWorkResult.Error("500 Internal Server Error", "500")
        )

        repository.processCheckout("checkout-1", googlePayData) {}.test {
            val error = awaitError()
            assertEquals("500 Internal Server Error", error.message)
        }
    }

    // endregion

    // region Google Pay request building

    @Test
    fun `isReadyToPayRequest does not contain tokenization or transaction info`() {
        val request = repository.isReadyToPayRequest()

        assertNotNull(request)
        assertEquals(2, request!!.getInt("apiVersion"))
        assertFalse(request.has("transactionInfo"))
        val cardMethod = request.getJSONArray("allowedPaymentMethods").getJSONObject(0)
        assertFalse(cardMethod.has("tokenizationSpecification"))
    }

    @Test
    fun `getPaymentDataRequest formats price and includes transaction info`() {
        val request = repository.getPaymentDataRequest(1050L)

        val transactionInfo = request.getJSONObject("transactionInfo")
        assertEquals("10.50", transactionInfo.getString("totalPrice"))
        assertEquals("FINAL", transactionInfo.getString("totalPriceStatus"))
        val cardMethod = request.getJSONArray("allowedPaymentMethods").getJSONObject(0)
        assertTrue(cardMethod.has("tokenizationSpecification"))
    }

    @Test
    fun `payment data request keys do not leak into ready to pay request`() {
        // Build the (richer) payment data request first…
        repository.getPaymentDataRequest(1999L)
        // …then ensure the readiness probe is not polluted by it.
        val readyRequest = repository.isReadyToPayRequest()

        assertNotNull(readyRequest)
        assertFalse(readyRequest!!.has("transactionInfo"))
        assertFalse(readyRequest.has("merchantInfo"))
    }

    // endregion

    // region Order management

    @Test
    fun `createFirestoreOrder delegates to datasource`() = runTest {
        coEvery { firestoreOrderDataSource.createOrder(any()) } returns Result.success(Unit)

        val order = Order(
            id = "order-1",
            customerName = "Jane",
            customerAddress = "1 rue du Fromage",
            customerBillingAddress = "1 rue du Fromage",
            deliveryDate = "25/12/2025",
            orderDate = "20/12/2025",
            products = mapOf("Comté" to 2),
            status = OrderStatus.PENDING,
            note = ""
        )

        assertTrue(repository.createFirestoreOrder(order).isSuccess)
    }

    @Test
    fun `updateFirestoreOrderStatus delegates to datasource`() = runTest {
        coEvery {
            firestoreOrderDataSource.updateOrder("order-1", OrderStatus.PAID)
        } returns Result.success(Unit)

        assertTrue(repository.updateFirestoreOrderStatus("order-1", OrderStatus.PAID).isSuccess)
    }

    // endregion
}
