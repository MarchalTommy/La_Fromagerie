package com.mtdevelopment.admin.domain.repository

import com.mtdevelopment.admin.domain.model.OptimizedRouteWithOrders
import com.mtdevelopment.core.model.Order

/**
 * Repository interface for interacting with Google Maps/Route API to optimize delivery paths.
 */
interface GoogleRouteRepository {
    /**
     * Calculates an optimized delivery path based on a list of addresses and daily orders.
     * @param addresses A list of destination addresses.
     * @param dailyOrders The list of orders associated with these addresses.
     * @return An [OptimizedRouteWithOrders] containing the sorted orders and route details.
     */
    suspend fun getOptimizedDeliveryPath(
        addresses: List<String>,
        dailyOrders: List<Order>
    ): OptimizedRouteWithOrders
}