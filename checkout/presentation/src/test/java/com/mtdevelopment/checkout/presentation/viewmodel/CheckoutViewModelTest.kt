package com.mtdevelopment.checkout.presentation.viewmodel

import com.google.android.gms.wallet.PaymentData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mtdevelopment.checkout.domain.model.CHECKOUT_STATUS
import com.mtdevelopment.checkout.domain.model.Checkout
import com.mtdevelopment.checkout.domain.model.LocalCheckoutInformation
import com.mtdevelopment.checkout.domain.model.NewCheckoutResult
import com.mtdevelopment.checkout.domain.usecase.ClearPendingPaymentFinalizationUseCase
import com.mtdevelopment.checkout.domain.usecase.CreateNewCheckoutUseCase
import com.mtdevelopment.checkout.domain.usecase.CreateNewOrderUseCase
import com.mtdevelopment.checkout.domain.usecase.CreatePaymentsClientUseCase
import com.mtdevelopment.checkout.domain.usecase.FetchAllowedPaymentMethods
import com.mtdevelopment.checkout.domain.usecase.GetCanUseGooglePayUseCase
import com.mtdevelopment.checkout.domain.usecase.GetCheckoutDataUseCase
import com.mtdevelopment.checkout.domain.usecase.GetIsPaymentSuccessUseCase
import com.mtdevelopment.checkout.domain.usecase.GetPaymentDataRequestUseCase
import com.mtdevelopment.checkout.domain.usecase.GetPreviouslyCreatedCheckoutUseCase
import com.mtdevelopment.checkout.domain.usecase.GetSavedOrderUseCase
import com.mtdevelopment.checkout.domain.usecase.GetSumUpPaymentLinkUseCase
import com.mtdevelopment.checkout.domain.usecase.ProcessSumUpCheckoutUseCase
import com.mtdevelopment.checkout.domain.usecase.ResetCheckoutStatusUseCase
import com.mtdevelopment.checkout.domain.usecase.SaveCheckoutReferenceUseCase
import com.mtdevelopment.checkout.domain.usecase.SaveCreatedCheckoutUseCase
import com.mtdevelopment.checkout.domain.usecase.SavePaymentStateUseCase
import com.mtdevelopment.checkout.domain.usecase.SchedulePaymentFinalizationUseCase
import com.mtdevelopment.checkout.domain.usecase.UpdateOrderStatus
import com.mtdevelopment.checkout.domain.usecase.VerifyHostedCheckoutStatusUseCase
import com.mtdevelopment.core.model.CartItem
import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus
import com.mtdevelopment.core.usecase.ClearCartUseCase
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    private val getIsConnectedUseCase: GetIsNetworkConnectedUseCase = mockk()
    private val fetchAllowedPaymentMethods: FetchAllowedPaymentMethods = mockk()
    private val createPaymentsClientUseCase: CreatePaymentsClientUseCase = mockk()
    private val getCheckoutDataUseCase: GetCheckoutDataUseCase = mockk()
    private val getCanUseGooglePayUseCase: GetCanUseGooglePayUseCase = mockk()
    private val getPaymentDataRequestUseCase: GetPaymentDataRequestUseCase = mockk()
    private val createNewCheckoutUseCase: CreateNewCheckoutUseCase = mockk()
    private val processSumUpCheckoutUseCase: ProcessSumUpCheckoutUseCase = mockk()
    private val saveCheckoutReferenceUseCase: SaveCheckoutReferenceUseCase = mockk(relaxed = true)
    private val getPreviouslyCreatedCheckoutUseCase: GetPreviouslyCreatedCheckoutUseCase = mockk()
    private val saveCreatedCheckoutUseCase: SaveCreatedCheckoutUseCase = mockk(relaxed = true)
    private val savePaymentStateUseCase: SavePaymentStateUseCase = mockk(relaxed = true)
    private val getIsPaymentSuccessUseCase: GetIsPaymentSuccessUseCase = mockk()
    private val clearCartUseCase: ClearCartUseCase = mockk(relaxed = true)
    private val resetCheckoutStatusUseCase: ResetCheckoutStatusUseCase = mockk(relaxed = true)
    private val createNewOrderUseCase: CreateNewOrderUseCase = mockk()
    private val updateOrderStatus: UpdateOrderStatus = mockk(relaxed = true)
    private val getSavedOrderUseCase: GetSavedOrderUseCase = mockk()
    private val schedulePaymentFinalizationUseCase: SchedulePaymentFinalizationUseCase =
        mockk(relaxed = true)
    private val clearPendingPaymentFinalizationUseCase: ClearPendingPaymentFinalizationUseCase =
        mockk(relaxed = true)
    private val getSumUpPaymentLinkUseCase: GetSumUpPaymentLinkUseCase = mockk()
    private val verifyHostedCheckoutStatusUseCase: VerifyHostedCheckoutStatusUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic(PaymentData::class)
        every { PaymentData.fromJson(any()) } returns mockk(relaxed = true)
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        every { getIsConnectedUseCase.invoke() } returns flowOf(true)
        every { fetchAllowedPaymentMethods.invoke() } returns JSONArray()
        every { createPaymentsClientUseCase.invoke() } returns mockk(relaxed = true)
        coEvery { getCanUseGooglePayUseCase.invoke() } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun buildViewModel() = CheckoutViewModel(
        getIsConnectedUseCase,
        fetchAllowedPaymentMethods,
        createPaymentsClientUseCase,
        Json { ignoreUnknownKeys = true },
        getCheckoutDataUseCase,
        getCanUseGooglePayUseCase,
        getPaymentDataRequestUseCase,
        createNewCheckoutUseCase,
        processSumUpCheckoutUseCase,
        saveCheckoutReferenceUseCase,
        getPreviouslyCreatedCheckoutUseCase,
        saveCreatedCheckoutUseCase,
        savePaymentStateUseCase,
        getIsPaymentSuccessUseCase,
        clearCartUseCase,
        resetCheckoutStatusUseCase,
        createNewOrderUseCase,
        updateOrderStatus,
        getSavedOrderUseCase,
        schedulePaymentFinalizationUseCase,
        clearPendingPaymentFinalizationUseCase,
        getSumUpPaymentLinkUseCase,
        verifyHostedCheckoutStatusUseCase
    )

    @Test
    fun `google pay readiness check enables button when available`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testScheduler.advanceUntilIdle()

        val state = viewModel.paymentScreenState.value
        assertTrue(state.isGooglePayAvailable)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `google pay readiness check surfaces error when unavailable`() = runTest(testDispatcher) {
        coEvery { getCanUseGooglePayUseCase.invoke() } returns false

        val viewModel = buildViewModel()
        testScheduler.advanceUntilIdle()

        val state = viewModel.paymentScreenState.value
        assertFalse(state.isGooglePayAvailable)
        assertNotNull(state.error)
    }

    @Test
    fun `updateBuyerEmail and updateCheckoutNote mutate state`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.updateBuyerEmail("jane@example.com")
        viewModel.updateCheckoutNote("Sonner au portail")

        val state = viewModel.paymentScreenState.value
        assertEquals("jane@example.com", state.buyerEmail)
        assertEquals("Sonner au portail", state.checkoutNote)
    }

    @Test
    fun `setGooglePayEnabled and debug setters mutate state`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.setGooglePayEnabled(false)
        viewModel.setPaymentSuccess(true)
        viewModel.setPaymentError("boom")

        val state = viewModel.paymentScreenState.value
        assertFalse(state.isGooglePayAvailable)
        assertTrue(state.isPaymentSuccess)
        assertEquals("boom", state.error)
    }

    @Test
    fun `updateUiState populates buyer and cart information`() = runTest(testDispatcher) {
        val cart = CartItems(
            cartItems = listOf(CartItem(name = "Comté", price = 1000L, quantity = 2)),
            totalPrice = 2000L
        )
        every { getCheckoutDataUseCase.invoke() } returns flowOf(
            LocalCheckoutInformation(
                buyerName = "Jane",
                buyerAddress = "1 rue du Fromage",
                cartItems = cart,
                totalPrice = 2000L,
                deliveryDate = 42L,
                billingAddress = "2 rue de la Facture"
            )
        )

        val viewModel = buildViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.updateUiState()
        testScheduler.advanceUntilIdle()

        val state = viewModel.paymentScreenState.value
        assertEquals("Jane", state.buyerName)
        assertEquals("1 rue du Fromage", state.buyerAddress)
        assertEquals("2 rue de la Facture", state.buyerBillingAddress)
        assertEquals(2000L, state.totalPrice)
        assertEquals(42L, state.deliveryDate)
        assertEquals(cart, state.cartItems)
        assertFalse(state.isLoading)
    }

    @Test
    fun `createCheckout reports success when checkout has a status`() = runTest(testDispatcher) {
        every {
            createNewCheckoutUseCase.invoke(any(), any(), any(), any(), any(), any())
        } returns flowOf(checkoutResult(status = "PENDING"))

        val viewModel = buildViewModel()
        testScheduler.advanceUntilIdle()

        var callbackResult: Boolean? = null
        viewModel.createCheckout { callbackResult = it }
        testScheduler.advanceUntilIdle()

        assertEquals(true, callbackResult)
        assertFalse(viewModel.paymentScreenState.value.isLoading)
        coVerify(exactly = 1) { saveCreatedCheckoutUseCase.invoke(any()) }
        coVerify(exactly = 1) { saveCheckoutReferenceUseCase.invoke(any()) }
    }

    @Test
    fun `createCheckout reports failure when checkout creation failed`() =
        runTest(testDispatcher) {
            every {
                createNewCheckoutUseCase.invoke(any(), any(), any(), any(), any(), any())
            } returns flowOf(checkoutResult(status = null))

            val viewModel = buildViewModel()
            testScheduler.advanceUntilIdle()

            var callbackResult: Boolean? = null
            viewModel.createCheckout { callbackResult = it }
            testScheduler.advanceUntilIdle()

            assertEquals(false, callbackResult)
            assertFalse(viewModel.paymentScreenState.value.isLoading)
        }

    @Test
    fun `getSumUpPaymentLink schedules hosted finalization before handing out the url`() =
        runTest(testDispatcher) {
            val cart = CartItems(
                cartItems = listOf(CartItem(name = "Comté", price = 1000L, quantity = 2)),
                totalPrice = 2000L
            )
            every { getCheckoutDataUseCase.invoke() } returns flowOf(
                LocalCheckoutInformation(
                    buyerName = "Jane",
                    buyerAddress = "1 rue du Fromage",
                    cartItems = cart,
                    totalPrice = 2000L,
                    deliveryDate = 42L,
                    billingAddress = "2 rue de la Facture"
                )
            )
            coEvery { createNewOrderUseCase.invoke(any()) } returns true
            coEvery { getSumUpPaymentLinkUseCase.invoke(any(), any()) } returns
                    Result.success("https://pay.sumup.com/hosted")

            val viewModel = buildViewModel()
            testScheduler.advanceUntilIdle()
            viewModel.updateUiState()
            testScheduler.advanceUntilIdle()
            viewModel.createOrder { }
            testScheduler.advanceUntilIdle()
            val orderId = viewModel.paymentScreenState.value.orderId!!

            var receivedUrl: String? = null
            viewModel.getSumUpPaymentLink { receivedUrl = it }
            testScheduler.advanceUntilIdle()

            assertEquals("https://pay.sumup.com/hosted", receivedUrl)
            coVerify(exactly = 1) {
                schedulePaymentFinalizationUseCase.invoke(
                    checkoutId = null,
                    orderId = orderId,
                    expectedAmountCents = 2000L
                )
            }
            assertFalse(viewModel.paymentScreenState.value.isLoading)
        }

    @Test
    fun `getSumUpPaymentLink does not schedule finalization when the link request fails`() =
        runTest(testDispatcher) {
            val cart = CartItems(
                cartItems = listOf(CartItem(name = "Comté", price = 1000L, quantity = 2)),
                totalPrice = 2000L
            )
            every { getCheckoutDataUseCase.invoke() } returns flowOf(
                LocalCheckoutInformation(
                    buyerName = "Jane",
                    buyerAddress = "1 rue du Fromage",
                    cartItems = cart,
                    totalPrice = 2000L,
                    deliveryDate = 42L,
                    billingAddress = "2 rue de la Facture"
                )
            )
            coEvery { createNewOrderUseCase.invoke(any()) } returns true
            coEvery { getSumUpPaymentLinkUseCase.invoke(any(), any()) } returns
                    Result.failure(IllegalStateException("backend down"))

            val viewModel = buildViewModel()
            testScheduler.advanceUntilIdle()
            viewModel.updateUiState()
            testScheduler.advanceUntilIdle()
            viewModel.createOrder { }
            testScheduler.advanceUntilIdle()

            var receivedUrl: String? = null
            viewModel.getSumUpPaymentLink { receivedUrl = it }
            testScheduler.advanceUntilIdle()

            assertNull(receivedUrl)
            coVerify(exactly = 0) { schedulePaymentFinalizationUseCase.invoke(any(), any(), any()) }
            assertNotNull(viewModel.paymentScreenState.value.error)
        }

    @Test
    fun `getSumUpPaymentLink aborts without order id instead of creating a blank checkout`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()
            testScheduler.advanceUntilIdle()

            var receivedUrl: String? = null
            viewModel.getSumUpPaymentLink { receivedUrl = it }
            testScheduler.advanceUntilIdle()

            assertNull(receivedUrl)
            coVerify(exactly = 0) { getSumUpPaymentLinkUseCase.invoke(any(), any()) }
            coVerify(exactly = 0) { schedulePaymentFinalizationUseCase.invoke(any(), any(), any()) }
            assertNotNull(viewModel.paymentScreenState.value.error)
        }

    @Test
    fun `verifySumUpWebCheckoutStatus finalizes the order on a matching PAID checkout`() =
        runTest(testDispatcher) {
            every { getSavedOrderUseCase.invoke() } returns flowOf(savedOrder(totalPrice = 2000L))
            every { verifyHostedCheckoutStatusUseCase.invoke("order-1", 2000L) } returns
                    flowOf(Result.success(domainCheckout(CHECKOUT_STATUS.PAID, amount = 20.0)))

            val viewModel = buildViewModel()
            testScheduler.advanceUntilIdle()

            viewModel.verifySumUpWebCheckoutStatus()
            testScheduler.advanceUntilIdle()

            val state = viewModel.paymentScreenState.value
            assertFalse(state.isLoading)
            assertNull(state.error)
            // The PAID branch ran: order marked PAID and cart cleared.
            coVerify(exactly = 1) {
                updateOrderStatus.invoke(orderId = "order-1", newStatus = OrderStatus.PAID)
            }
            coVerify(exactly = 1) { clearCartUseCase.invoke() }
        }

    @Test
    fun `verifySumUpWebCheckoutStatus keeps the cart and reports failure on a FAILED checkout`() =
        runTest(testDispatcher) {
            every { getSavedOrderUseCase.invoke() } returns flowOf(savedOrder(totalPrice = 2000L))
            every { verifyHostedCheckoutStatusUseCase.invoke("order-1", 2000L) } returns
                    flowOf(Result.success(domainCheckout(CHECKOUT_STATUS.FAILED, amount = 20.0)))

            val viewModel = buildViewModel()
            testScheduler.advanceUntilIdle()

            viewModel.verifySumUpWebCheckoutStatus()
            testScheduler.advanceUntilIdle()

            val state = viewModel.paymentScreenState.value
            assertFalse(state.isLoading)
            assertFalse(state.isPaymentSuccess)
            assertNotNull(state.error)
            assertTrue(state.error!!.contains("panier est conservé"))
            coVerify(exactly = 0) { clearCartUseCase.invoke() }
        }

    @Test
    fun `verifySumUpWebCheckoutStatus does not claim not-charged when the outcome is unknown`() =
        runTest(testDispatcher) {
            every { getSavedOrderUseCase.invoke() } returns flowOf(savedOrder(totalPrice = 2000L))
            every { verifyHostedCheckoutStatusUseCase.invoke("order-1", 2000L) } returns
                    flowOf(Result.failure(Exception("unresolved")))

            val viewModel = buildViewModel()
            testScheduler.advanceUntilIdle()

            viewModel.verifySumUpWebCheckoutStatus()
            testScheduler.advanceUntilIdle()

            val state = viewModel.paymentScreenState.value
            assertFalse(state.isLoading)
            assertFalse(state.isPaymentSuccess)
            assertNotNull(state.error)
            // Honest ambiguity: verifying, do not pay again — never "you were not charged".
            assertTrue(state.error!!.contains("Nous vérifions"))
            coVerify(exactly = 0) { clearCartUseCase.invoke() }
        }

    @Test
    fun `verifySumUpWebCheckoutStatus fails closed when the order total is unknown`() =
        runTest(testDispatcher) {
            every { getSavedOrderUseCase.invoke() } returns flowOf(savedOrder(totalPrice = null))

            val viewModel = buildViewModel()
            testScheduler.advanceUntilIdle()

            viewModel.verifySumUpWebCheckoutStatus()
            testScheduler.advanceUntilIdle()

            val state = viewModel.paymentScreenState.value
            assertFalse(state.isLoading)
            assertNotNull(state.error)
            // Without the total we cannot enforce amount integrity: never poll/accept a PAID.
            coVerify(exactly = 0) { verifyHostedCheckoutStatusUseCase.invoke(any(), any()) }
        }

    @Test
    fun `verifySumUpWebCheckoutStatus reports when no order is pending`() =
        runTest(testDispatcher) {
            every { getSavedOrderUseCase.invoke() } returns emptyFlow()

            val viewModel = buildViewModel()
            testScheduler.advanceUntilIdle()

            viewModel.verifySumUpWebCheckoutStatus()
            testScheduler.advanceUntilIdle()

            val state = viewModel.paymentScreenState.value
            assertFalse(state.isLoading)
            assertNotNull(state.error)
            coVerify(exactly = 0) { verifyHostedCheckoutStatusUseCase.invoke(any(), any()) }
        }

    private fun savedOrder(totalPrice: Long?) = Order(
        id = "order-1",
        customerName = "Jane",
        customerAddress = "1 rue du Fromage",
        customerBillingAddress = "2 rue de la Facture",
        deliveryDate = "10/07/2026",
        orderDate = "09/07/2026",
        products = mapOf("Comté" to 2),
        status = OrderStatus.PENDING,
        note = null,
        totalPrice = totalPrice
    )

    private fun domainCheckout(status: CHECKOUT_STATUS, amount: Double) = Checkout(
        amount = amount,
        checkoutReference = "order-1",
        currency = "EUR",
        merchantCode = "MERCHANT",
        status = status
    )

    private fun checkoutResult(status: String?) = NewCheckoutResult(
        amount = 10.5,
        checkoutReference = "ref-1",
        currency = "EUR",
        customerId = null,
        date = null,
        description = null,
        id = "checkout-1",
        mandate = null,
        merchantCode = null,
        merchantCountry = null,
        payToEmail = null,
        returnUrl = null,
        status = status,
        transactions = null,
        validUntil = null
    )
}
