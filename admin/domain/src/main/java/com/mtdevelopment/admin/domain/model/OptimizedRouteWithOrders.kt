package com.mtdevelopment.admin.domain.model

import androidx.annotation.Keep
import com.mtdevelopment.core.model.Order
import kotlinx.serialization.Serializable

/**
 * Data class representing an optimized delivery route along with its associated orders.
 * @property optimizedRoute A list of (latitude, longitude) pairs defining the path to follow.
 * @property optimizedOrders The list of orders, sorted in the optimized delivery sequence.
 */
@Keep
@Serializable
data class OptimizedRouteWithOrders(
    val optimizedRoute: List<Pair<Double, Double>>,
    val optimizedOrders: List<Order>
)
