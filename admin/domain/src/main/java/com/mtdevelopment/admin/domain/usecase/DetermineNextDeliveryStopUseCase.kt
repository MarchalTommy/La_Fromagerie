package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.model.OptimizedRouteWithOrders
import com.mtdevelopment.admin.domain.repository.CurrentLocation
import com.mtdevelopment.core.model.Order

class DetermineNextDeliveryStopUseCase() {

    // A simple threshold for considering an admin "at" or "past" a stop. Adjust as needed.
    private val AT_STOP_THRESHOLD_METERS = 50.0f

    operator fun invoke(
        currentAdminLocation: CurrentLocation,
        optimizedRouteWaypoints: OptimizedRouteWithOrders,
    ): Order? {
        if (optimizedRouteWaypoints.optimizedOrders.isEmpty() || optimizedRouteWaypoints.optimizedRoute.isEmpty()) {
            return null
        }

        // Let's find the first waypoint in the optimized list that the admin hasn't reached yet.
        for ((index, waypoint) in optimizedRouteWaypoints.optimizedRoute.withIndex()) {
            val distanceToWaypoint = calculateDistance(
                currentAdminLocation.latitude, currentAdminLocation.longitude,
                waypoint.first, waypoint.second
            )

            // If the admin is close to this waypoint, or has passed it recently,
            // we might consider the *next* one in the list as the "next stop".
            // This logic needs careful refinement.
            // For now, let's find the *closest* future waypoint.

            if (distanceToWaypoint > AT_STOP_THRESHOLD_METERS) {
                // This waypoint is still ahead.
                // Now, we need to find which order corresponds to this waypoint.

                return findNextClosestOrder(
                    optimizedRouteWaypoints.optimizedOrders,
                    optimizedRouteWaypoints.optimizedRoute,
                    index
                )
            }
        }
        return null // All waypoints might have been "reached"
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    private fun findNextClosestOrder(
        orders: List<Order>,
        waypoints: List<Pair<Double, Double>>,
        startIndex: Int
    ): Order? {
        // Find the order corresponding to the waypoint at startIndex or subsequent ones
        for (i in startIndex until waypoints.size - 1) {
            val orderForWaypoint = orders[i]
            return orderForWaypoint
        }
        return null
    }
}