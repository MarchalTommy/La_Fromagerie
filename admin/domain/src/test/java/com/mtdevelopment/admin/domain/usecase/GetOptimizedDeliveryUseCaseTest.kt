package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.model.OptimizedRouteWithOrders
import com.mtdevelopment.admin.domain.repository.AdminDatastorePreference
import com.mtdevelopment.admin.domain.repository.GoogleRouteRepository
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetOptimizedDeliveryUseCaseTest {

    private lateinit var googleRouteRepository: GoogleRouteRepository
    private lateinit var adminDatastorePreference: AdminDatastorePreference
    private lateinit var useCase: GetOptimizedDeliveryUseCase

    private fun order(id: String) = Order(
        id = id,
        customerName = "Jane",
        customerAddress = "1 rue du Fromage",
        customerBillingAddress = "1 rue du Fromage",
        deliveryDate = "25/12/2025",
        orderDate = "20/12/2025",
        products = mapOf("Comté" to 1),
        status = OrderStatus.PAID,
        note = null
    )

    private val orders = listOf(order("a"), order("b"))
    private val addresses = listOf("1 rue du Fromage", "2 rue du Lait")

    private val cachedRoute = OptimizedRouteWithOrders(
        optimizedRoute = listOf(46.9 to 6.35),
        optimizedOrders = orders
    )
    private val freshRoute = OptimizedRouteWithOrders(
        optimizedRoute = listOf(47.0 to 6.40),
        optimizedOrders = orders.reversed()
    )

    @Before
    fun setUp() {
        googleRouteRepository = mockk()
        adminDatastorePreference = mockk()
        useCase = GetOptimizedDeliveryUseCase(googleRouteRepository, adminDatastorePreference)
    }

    @Test
    fun `returns cached route when it matches the daily orders`() = runTest {
        every { adminDatastorePreference.dailyDeliveryPathGeocodedFlow } returns flowOf(cachedRoute)

        val result = useCase(addresses, orders)

        assertEquals(cachedRoute, result)
        coVerify(exactly = 0) { googleRouteRepository.getOptimizedDeliveryPath(any(), any()) }
    }

    @Test
    fun `fetches fresh route when no cache exists`() = runTest {
        every { adminDatastorePreference.dailyDeliveryPathGeocodedFlow } returns flowOf(null)
        coEvery {
            googleRouteRepository.getOptimizedDeliveryPath(addresses, orders)
        } returns freshRoute

        assertEquals(freshRoute, useCase(addresses, orders))
    }

    @Test
    fun `fetches fresh route when cached orders differ from daily orders`() = runTest {
        val staleCache = cachedRoute.copy(optimizedOrders = listOf(order("a"), order("removed")))
        every { adminDatastorePreference.dailyDeliveryPathGeocodedFlow } returns flowOf(staleCache)
        coEvery {
            googleRouteRepository.getOptimizedDeliveryPath(addresses, orders)
        } returns freshRoute

        assertEquals(freshRoute, useCase(addresses, orders))
        coVerify(exactly = 1) { googleRouteRepository.getOptimizedDeliveryPath(addresses, orders) }
    }

    @Test
    fun `forceRefresh bypasses a valid cache`() = runTest {
        every { adminDatastorePreference.dailyDeliveryPathGeocodedFlow } returns flowOf(cachedRoute)
        coEvery {
            googleRouteRepository.getOptimizedDeliveryPath(addresses, orders)
        } returns freshRoute

        assertEquals(freshRoute, useCase(addresses, orders, forceRefresh = true))
    }
}
