package com.mtdevelopment.admin.data.repository

import com.mtdevelopment.admin.data.source.GoogleRouteDataSource
import com.mtdevelopment.admin.domain.model.OptimizedRouteWithOrders
import com.mtdevelopment.admin.domain.repository.GoogleRouteRepository
import com.mtdevelopment.core.domain.reorderList
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.util.NetWorkResult

/**
 * Implementation of [GoogleRouteRepository] that uses [GoogleRouteDataSource] to fetch optimized routes.
 * It maps the network response to domain models and reorders the orders list based on the optimized waypoint indexes provided by Google.
 */
class GoogleRouteRepositoryImpl(
    private val googleRouteDataSource: GoogleRouteDataSource
) : GoogleRouteRepository {

    /**
     * Fetches an optimized route from Google Maps/Route API and reorders the daily orders accordingly.
     * The process:
     * 1. Calls [GoogleRouteDataSource] with the list of addresses.
     * 2. Extracts waypoint indexes from the response. These indexes indicate the new order of the intermediate stops.
     * 3. Extracts geographical coordinates (lat/lng) for each leg of the route.
     * 4. Reorders the [dailyOrders] using the [reorderList] utility and the provided indexes.
     */
    override suspend fun getOptimizedDeliveryPath(
        addresses: List<String>,
        dailyOrders: List<Order>
    ): OptimizedRouteWithOrders {

        val result = googleRouteDataSource.getOptimizedRoute(addressesList = addresses)
        val response = (result as? NetWorkResult.Success)?.data

        // The optimizedIntermediateWaypointIndex contains the new ordering of the waypoints.
        val indexes =
            response?.routes?.firstOrNull()?.optimizedIntermediateWaypointIndex ?: listOf()

        val finalItem = OptimizedRouteWithOrders(
            // Mapping each leg's end location to a Pair of (Lat, Lng)
            optimizedRoute = response?.routes?.firstOrNull()?.legs?.mapNotNull {
                Pair(
                    it?.endLocation?.latLng?.latitude ?: 0.0,
                    it?.endLocation?.latLng?.longitude ?: 0.0
                )
            } ?: listOf(),
            // Reordering the orders based on the optimized indexes.
            // reorderList validates the indexes (size, bounds, duplicates) and falls back to the
            // original order when waypoints and orders do not align (e.g. several orders at the
            // same address), so a malformed Google response can never crash or drop orders.
            optimizedOrders = reorderList(dailyOrders, indexes)
        )


        return finalItem
    }

}