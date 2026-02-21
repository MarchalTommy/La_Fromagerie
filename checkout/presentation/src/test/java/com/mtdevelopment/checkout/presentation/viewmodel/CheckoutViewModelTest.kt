package com.mtdevelopment.checkout.presentation.viewmodel

import android.util.Log
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentsClient
import com.mtdevelopment.checkout.domain.model.NewCheckoutResult
import com.mtdevelopment.checkout.domain.usecase.*
import com.mtdevelopment.checkout.presentation.rules.MainDispatcherRule
import com.mtdevelopment.core.model.CartItem
import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.checkout.domain.model.LocalCheckoutInformation
import com.mtdevelopment.core.usecase.ClearCartUseCase
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getIsConnectedUseCase: GetIsNetworkConnectedUseCase = mockk()
    private val fetchAllowedPaymentMethods: FetchAllowedPaymentMethods = mockk()
    private val createPaymentsClientUseCase: CreatePaymentsClientUseCase = mockk()
    private val json: Json = mockk()
    private val getCheckoutDataUseCase: GetCheckoutDataUseCase = mockk()
    private val getCanUseGooglePayUseCase: GetCanUseGooglePayUseCase = mockk()
    private val getPaymentDataRequestUseCase: GetPaymentDataRequestUseCase = mockk()
    private val createNewCheckoutUseCase: CreateNewCheckoutUseCase = mockk()
    private val processSumUpCheckoutUseCase: ProcessSumUpCheckoutUseCase = mockk()
    private val saveCheckoutReferenceUseCase: SaveCheckoutReferenceUseCase = mockk()
    private val getPreviouslyCreatedCheckoutUseCase: GetPreviouslyCreatedCheckoutUseCase = mockk()
    private val saveCreatedCheckoutUseCase: SaveCreatedCheckoutUseCase = mockk()
    private val savePaymentStateUseCase: SavePaymentStateUseCase = mockk()
    private val getIsPaymentSuccessUseCase: GetIsPaymentSuccessUseCase = mockk()
    private val clearCartUseCase: ClearCartUseCase = mockk()
    private val resetCheckoutStatusUseCase: ResetCheckoutStatusUseCase = mockk()
    private val createNewOrderUseCase: CreateNewOrderUseCase = mockk()
    private val updateOrderStatus: UpdateOrderStatus = mockk()
    private val getSavedOrderUseCase: GetSavedOrderUseCase = mockk()

    private lateinit var viewModel: CheckoutViewModel

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0

        mockkStatic(PaymentData::class)
        every { PaymentData.fromJson(any()) } returns mockk()

        every { getIsConnectedUseCase.invoke() } returns flowOf(true)
        every { fetchAllowedPaymentMethods.invoke() } returns mockk<JSONArray> {
            every { toString() } returns "[]"
        }
        every { createPaymentsClientUseCase.invoke() } returns mockk<PaymentsClient>()

        // Mock getCanUseGooglePayUseCase for init block
        coEvery { getCanUseGooglePayUseCase.invoke() } returns true

        // Mock getCheckoutDataUseCase for updateUiState
         every { getCheckoutDataUseCase.invoke() } returns flowOf(
             LocalCheckoutInformation(
                 buyerName = "John Doe",
                 buyerAddress = "123 Main St",
                 billingAddress = "123 Main St",
                 totalPrice = 1000,
                 cartItems = CartItems(listOf(CartItem("Product A", 10.0, 1, "", "")))
             )
         )

        viewModel = CheckoutViewModel(
            getIsConnectedUseCase,
            fetchAllowedPaymentMethods,
            createPaymentsClientUseCase,
            json,
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
            getSavedOrderUseCase
        )
    }

    @Test
    fun `updateNote updates state correctly`() {
        val note = "Please leave at the door"
        viewModel.updateNote(note)
        assertEquals(note, viewModel.paymentScreenState.value.note)
    }

    @Test
    fun `updateBillingAddress updates state correctly`() {
        val address = "456 Billing Ave"
        viewModel.updateBillingAddress(address)
        assertEquals(address, viewModel.paymentScreenState.value.billingAddress)
    }

    @Test
    fun `createOrder calls use case with correct note and billing address`() = runTest {
        // Arrange
        val note = "Special Note"
        val billingAddress = "Billing St"
        viewModel.updateNote(note)
        viewModel.updateBillingAddress(billingAddress)
        viewModel.updateUiState() // Load initial data

        coEvery { createNewOrderUseCase.invoke(any()) } returns true

        // Act
        viewModel.createOrder { isSuccess ->
            assert(isSuccess)
        }

        // Assert
        coVerify {
            createNewOrderUseCase.invoke(match { order ->
                order.note == note &&
                order.customerBillingAddress == billingAddress &&
                order.customerName == "John Doe"
            })
        }
    }

    @Test
    fun `createCheckout passes billing address correctly`() = runTest {
        // Arrange
        val billingAddress = "Billing St"
        viewModel.updateBillingAddress(billingAddress)
        viewModel.updateUiState()

        val checkoutResult = NewCheckoutResult(status = "PENDING")
        coEvery { createNewCheckoutUseCase.invoke(any(), any(), any(), any(), any(), any(), any()) } returns flowOf(checkoutResult)
        coEvery { saveCheckoutReferenceUseCase.invoke(any()) } just Runs
        coEvery { saveCreatedCheckoutUseCase.invoke(any()) } just Runs

        // Act
        viewModel.createCheckout { isSuccess ->
             assert(isSuccess)
        }

        // Assert
        coVerify {
            createNewCheckoutUseCase.invoke(
                amount = any(),
                description = any(),
                buyerName = any(),
                buyerAddress = any(),
                billingAddress = billingAddress,
                buyerEmail = any(),
                reference = any()
            )
        }
    }
}
