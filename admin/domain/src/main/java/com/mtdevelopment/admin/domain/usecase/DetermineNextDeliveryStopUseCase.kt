package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.CurrentLocation
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.delivery.domain.repository.AddressApiRepository

class DetermineNextDeliveryStopUseCase(
    private val addressApiRepository: AddressApiRepository
) {

    // A simple threshold for considering an admin "at" or "past" a stop. Adjust as needed.
    private val AT_STOP_THRESHOLD_METERS = 150.0f

    suspend operator fun invoke(
        currentAdminLocation: CurrentLocation,
        todaysOrders: List<Order>,
        optimizedRouteWaypoints: List<Pair<Double, Double>>,
    ): Order? {
        if (todaysOrders.isEmpty() || optimizedRouteWaypoints.isEmpty()) {
            return null
        }

        // This logic is a placeholder and needs to be robust.
        // It assumes `optimizedRouteWaypoints` are the geocoded addresses from `todaysOrders`
        // in the sequence determined by GetOptimizedDeliveryUseCase.
        // You'll need a way to map waypoints back to their original Order objects.
        // For simplicity, let's assume `optimizedRouteWaypoints` corresponds 1:1 in order
        // with a filtered/sorted list of `todaysOrders` that was used to generate the route.
        // This mapping is crucial and depends on how GetOptimizedDeliveryUseCase works with addresses.

        // Let's find the first waypoint in the optimized list that the admin hasn't reached yet.
        for ((index, waypoint) in optimizedRouteWaypoints.withIndex()) {
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
                // This requires that the `optimizedRouteWaypoints` can be reliably mapped back
                // to the `todaysOrders`.
                // If `GetOptimizedDeliveryUseCase` returns indices, use those.
                // If not, you might need to geocode addresses from `todaysOrders` and match.

                // TODO: REARRANGE TODAYSORDERS WITH THE INDEXES TO MATCH THE WAYPOINTS

                // Placeholder: assuming a direct mapping for simplicity
                // You would need a more robust way to get the order based on the waypoint
                // e.g. if GetOptimizedDeliveryUseCase returns the optimized ORDER of original addresses
                // you could try to find an order whose address is closest to this waypoint.
                val orderForThisWaypoint = todaysOrders.find { order ->
                    val orderLocation = geocodeAddress(order.customerAddress)
                    orderLocation != null &&
                            calculateDistance(
                                waypoint.first,
                                waypoint.second,
                                orderLocation.first,
                                orderLocation.second
                            ) < AT_STOP_THRESHOLD_METERS
                }
                return orderForThisWaypoint ?: findNextClosestOrder(
                    todaysOrders,
                    optimizedRouteWaypoints,
                    index
                )
            }
        }
        return null // All waypoints might have been "reached"
    }

    private suspend fun geocodeAddress(address: String): Pair<Double, Double>? {
        val result = addressApiRepository.geocodeAddress(address)
        if (result != null) {
            return Pair(result.location.latitude, result.location.longitude)
        }
        return null
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    private suspend fun findNextClosestOrder(
        orders: List<Order>,
        waypoints: List<Pair<Double, Double>>,
        startIndex: Int
    ): Order? {
        // Find the order corresponding to the waypoint at startIndex or subsequent ones
        for (i in startIndex until waypoints.size) {
            val waypoint = waypoints[i]
            val orderForWaypoint = orders.find { order ->
                val orderLocation = geocodeAddress(order.customerAddress)
                orderLocation != null &&
                        calculateDistance(
                            waypoint.first,
                            waypoint.second,
                            orderLocation.first,
                            orderLocation.second
                        ) < AT_STOP_THRESHOLD_METERS
            }
            if (orderForWaypoint != null) return orderForWaypoint
        }
        return null
    }
}