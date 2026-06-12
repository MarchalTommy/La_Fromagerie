package com.mtdevelopment.admin.data.repository

import android.util.Log
import com.mtdevelopment.admin.data.model.GoogleOptimizedComputedRouteResponse
import com.mtdevelopment.admin.data.source.GoogleRouteDataSource
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus
import com.mtdevelopment.core.util.NetWorkResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GoogleRouteRepositoryImplTest {

    private lateinit var dataSource: GoogleRouteDataSource
    private lateinit var repository: GoogleRouteRepositoryImpl

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

    private val orders = listOf(order("a"), order("b"), order("c"))
    private val addresses = listOf("addr a", "addr b", "addr c")

    private fun leg(lat: Double, lng: Double) =
        GoogleOptimizedComputedRouteResponse.Route.Leg(
            endLocation = GoogleOptimizedComputedRouteResponse.Route.Leg.EndLocation(
                latLng = GoogleOptimizedComputedRouteResponse.Route.Leg.EndLocation.LatLng(
                    latitude = lat,
                    longitude = lng
                )
            )
        )

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0

        dataSource = mockk()
        repository = GoogleRouteRepositoryImpl(dataSource)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `maps legs to coordinates and reorders orders with optimized indexes`() = runTest {
        coEvery { dataSource.getOptimizedRoute(addresses) } returns NetWorkResult.Success(
            GoogleOptimizedComputedRouteResponse(
                routes = listOf(
                    GoogleOptimizedComputedRouteResponse.Route(
                        legs = listOf(leg(1.0, 1.1), leg(2.0, 2.1), leg(3.0, 3.1)),
                        // order "a" goes to position 2, "b" to 0, "c" to 1
                        optimizedIntermediateWaypointIndex = listOf(2, 0, 1)
                    )
                )
            )
        )

        val result = repository.getOptimizedDeliveryPath(addresses, orders)

        assertEquals(listOf(1.0 to 1.1, 2.0 to 2.1, 3.0 to 3.1), result.optimizedRoute)
        assertEquals(listOf("b", "c", "a"), result.optimizedOrders.map { it.id })
    }

    @Test
    fun `keeps original order when optimized indexes are invalid`() = runTest {
        coEvery { dataSource.getOptimizedRoute(addresses) } returns NetWorkResult.Success(
            GoogleOptimizedComputedRouteResponse(
                routes = listOf(
                    GoogleOptimizedComputedRouteResponse.Route(
                        legs = listOf(leg(1.0, 1.1)),
                        // Duplicated index: not a valid permutation
                        optimizedIntermediateWaypointIndex = listOf(0, 0, 1)
                    )
                )
            )
        )

        val result = repository.getOptimizedDeliveryPath(addresses, orders)

        assertEquals(listOf("a", "b", "c"), result.optimizedOrders.map { it.id })
    }

    @Test
    fun `single destination with missing optimized indexes keeps the only order`() = runTest {
        // Regression test: with a single destination, Google can omit the optimized index list
        // entirely. The old reorderList implementation crashed with IndexOutOfBoundsException.
        coEvery { dataSource.getOptimizedRoute(listOf("addr a")) } returns NetWorkResult.Success(
            GoogleOptimizedComputedRouteResponse(
                routes = listOf(
                    GoogleOptimizedComputedRouteResponse.Route(
                        legs = listOf(leg(1.0, 1.1), leg(0.0, 0.1)),
                        optimizedIntermediateWaypointIndex = null
                    )
                )
            )
        )

        val result = repository.getOptimizedDeliveryPath(listOf("addr a"), listOf(order("a")))

        assertEquals(listOf("a"), result.optimizedOrders.map { it.id })
        assertEquals(2, result.optimizedRoute.size)
    }

    @Test
    fun `keeps original order and empty route on network error`() = runTest {
        coEvery { dataSource.getOptimizedRoute(addresses) } returns NetWorkResult.Error(
            "timeout", "EXCEPTION"
        )

        val result = repository.getOptimizedDeliveryPath(addresses, orders)

        assertTrue(result.optimizedRoute.isEmpty())
        assertEquals(listOf("a", "b", "c"), result.optimizedOrders.map { it.id })
    }
}
