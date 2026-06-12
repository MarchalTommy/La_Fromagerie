package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetAllOrdersUseCaseTest {

    private val repository: FirebaseAdminRepository = mockk()
    private val useCase = GetAllOrdersUseCase(repository)

    private fun order(id: String, deliveryDate: String, status: OrderStatus) = Order(
        id = id,
        customerName = "Jean Dupont",
        customerAddress = "1 Rue de la Paix, 75002 Paris",
        customerBillingAddress = "1 Rue de la Paix, 75002 Paris",
        deliveryDate = deliveryDate,
        orderDate = "01/01/2026",
        products = mapOf("Comté AOP" to 2),
        status = status,
        note = null,
        isManuallyAdded = false
    )

    private fun givenRepositoryReturns(orders: List<Order>?) {
        coEvery { repository.getAllOrders(any()) } coAnswers {
            firstArg<(List<Order>?) -> Unit>().invoke(orders)
        }
    }

    @Test
    fun `orders are sorted by delivery date descending`() = runTest {
        givenRepositoryReturns(
            listOf(
                order("old", "02/01/2026", OrderStatus.PAID),
                order("newest", "15/03/2026", OrderStatus.PAID),
                order("middle", "20/02/2026", OrderStatus.PAID)
            )
        )

        var result: List<Order>? = null
        useCase.invoke { result = it }

        assertEquals(listOf("newest", "middle", "old"), result?.map { it.id })
    }

    @Test
    fun `orders with failed payment never reach the result`() = runTest {
        givenRepositoryReturns(
            listOf(
                order("paid", "02/01/2026", OrderStatus.PAID),
                order("failed", "15/03/2026", OrderStatus.CANCELED),
                order("pending", "20/02/2026", OrderStatus.PENDING)
            )
        )

        var result: List<Order>? = null
        useCase.invoke { result = it }

        assertEquals(listOf("pending", "paid"), result?.map { it.id })
    }

    @Test
    fun `repository failure propagates null`() = runTest {
        givenRepositoryReturns(null)

        var result: List<Order>? = emptyList()
        useCase.invoke { result = it }

        assertNull(result)
    }
}
