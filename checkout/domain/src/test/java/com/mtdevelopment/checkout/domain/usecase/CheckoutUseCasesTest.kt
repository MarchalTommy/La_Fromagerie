package com.mtdevelopment.checkout.domain.usecase

import app.cash.turbine.test
import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference
import com.mtdevelopment.checkout.domain.repository.PaymentRepository
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutUseCasesTest {

    private val order = Order(
        id = "order-1",
        customerName = "Jane",
        customerAddress = "1 rue du Fromage",
        customerBillingAddress = "1 rue du Fromage",
        deliveryDate = "25/12/2025",
        orderDate = "20/12/2025",
        products = mapOf("Comté" to 2),
        status = OrderStatus.PENDING,
        note = null
    )

    @Test
    fun `CreateNewOrderUseCase saves order locally before creating it remotely`() = runTest {
        val preference = mockk<CheckoutDatastorePreference>(relaxed = true)
        val repository = mockk<PaymentRepository> {
            coEvery { createFirestoreOrder(order) } returns Result.success(Unit)
        }

        val result = CreateNewOrderUseCase(repository, preference).invoke(order)

        assertTrue(result)
        coVerifyOrder {
            preference.saveOrder(order)
            repository.createFirestoreOrder(order)
        }
    }

    @Test
    fun `CreateNewOrderUseCase returns false when remote creation fails`() = runTest {
        val preference = mockk<CheckoutDatastorePreference>(relaxed = true)
        val repository = mockk<PaymentRepository> {
            coEvery { createFirestoreOrder(order) } returns Result.failure(Exception("boom"))
        }

        assertFalse(CreateNewOrderUseCase(repository, preference).invoke(order))
    }

    @Test
    fun `UpdateOrderStatus reflects repository result`() = runTest {
        val repository = mockk<PaymentRepository> {
            coEvery {
                updateFirestoreOrderStatus("order-1", OrderStatus.PAID)
            } returns Result.success(Unit)
            coEvery {
                updateFirestoreOrderStatus("order-2", OrderStatus.PAID)
            } returns Result.failure(Exception("not found"))
        }

        assertTrue(UpdateOrderStatus(repository).invoke("order-1", OrderStatus.PAID))
        assertFalse(UpdateOrderStatus(repository).invoke("order-2", OrderStatus.PAID))
    }

    @Test
    fun `GetSavedOrderUseCase filters out null orders`() = runTest {
        val preference = mockk<CheckoutDatastorePreference> {
            every { orderFlow } returns flowOf(null, order)
        }

        GetSavedOrderUseCase(preference).invoke().test {
            assertEquals(order, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `SaveCheckoutReferenceUseCase delegates to preference`() = runTest {
        val preference = mockk<CheckoutDatastorePreference>(relaxed = true)

        SaveCheckoutReferenceUseCase(preference).invoke("ref-1")

        coVerify(exactly = 1) { preference.saveCheckoutReference("ref-1") }
    }

    @Test
    fun `SavePaymentStateUseCase delegates to preference`() = runTest {
        val preference = mockk<CheckoutDatastorePreference>(relaxed = true)

        SavePaymentStateUseCase(preference).invoke(true)

        coVerify(exactly = 1) { preference.setIsCheckoutSuccessful(true) }
    }

    @Test
    fun `ResetCheckoutStatusUseCase delegates to preference`() = runTest {
        val preference = mockk<CheckoutDatastorePreference>(relaxed = true)

        ResetCheckoutStatusUseCase(preference).invoke()

        coVerify(exactly = 1) { preference.resetCheckoutStatus() }
    }

    @Test
    fun `GetIsPaymentSuccessUseCase exposes preference flow`() = runTest {
        val preference = mockk<CheckoutDatastorePreference> {
            every { isCheckoutSuccessfulFlow } returns flowOf(false, true)
        }

        GetIsPaymentSuccessUseCase(preference).invoke().test {
            assertFalse(awaitItem())
            assertTrue(awaitItem())
            awaitComplete()
        }
    }
}
