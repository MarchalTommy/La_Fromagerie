package com.mtdevelopment.admin.domain.usecase

import android.location.Location
import com.mtdevelopment.admin.domain.model.OptimizedRouteWithOrders
import com.mtdevelopment.admin.domain.repository.CurrentLocation
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DetermineNextDeliveryStopUseCaseTest {

    private val useCase = DetermineNextDeliveryStopUseCase()

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

    @Before
    fun setUp() {
        mockkStatic(Location::class)
        // Simple planar approximation: 1 degree of latitude/longitude == 111_000 meters.
        every {
            Location.distanceBetween(any(), any(), any(), any(), any())
        } answers {
            val lat1 = arg<Double>(0)
            val lon1 = arg<Double>(1)
            val lat2 = arg<Double>(2)
            val lon2 = arg<Double>(3)
            val results = arg<FloatArray>(4)
            val dLat = (lat2 - lat1) * 111_000.0
            val dLon = (lon2 - lon1) * 111_000.0
            results[0] = Math.sqrt(dLat * dLat + dLon * dLon).toFloat()
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(Location::class)
    }

    @Test
    fun `returns first order when admin is far from every stop`() {
        val route = OptimizedRouteWithOrders(
            optimizedRoute = listOf(47.0 to 6.0, 47.1 to 6.1),
            optimizedOrders = listOf(order("first"), order("second"))
        )

        val next = useCase(CurrentLocation(46.0, 5.0), route)

        assertEquals("first", next?.id)
    }

    @Test
    fun `returns second order when the first stop is already reached`() {
        val route = OptimizedRouteWithOrders(
            optimizedRoute = listOf(47.0 to 6.0, 47.1 to 6.1),
            optimizedOrders = listOf(order("first"), order("second"))
        )

        // Standing on the first stop (distance 0 <= 50m threshold)
        val next = useCase(CurrentLocation(47.0, 6.0), route)

        assertEquals("second", next?.id)
    }

    @Test
    fun `returns null when every stop is reached`() {
        val route = OptimizedRouteWithOrders(
            optimizedRoute = listOf(47.0 to 6.0),
            optimizedOrders = listOf(order("only"))
        )

        assertNull(useCase(CurrentLocation(47.0, 6.0), route))
    }

    @Test
    fun `returns null when route or orders are empty`() {
        assertNull(
            useCase(
                CurrentLocation(47.0, 6.0),
                OptimizedRouteWithOrders(emptyList(), emptyList())
            )
        )
        assertNull(
            useCase(
                CurrentLocation(47.0, 6.0),
                OptimizedRouteWithOrders(listOf(47.0 to 6.0), emptyList())
            )
        )
    }

    @Test
    fun `final return-home leg never yields an order`() {
        // One delivery stop + the final leg back to the farm (no matching order)
        val route = OptimizedRouteWithOrders(
            optimizedRoute = listOf(47.0 to 6.0, 46.5 to 5.5),
            optimizedOrders = listOf(order("only"))
        )

        // Standing at the delivery stop: the remaining waypoint is the farm, with no order
        assertNull(useCase(CurrentLocation(47.0, 6.0), route))
    }
}
