package com.mtdevelopment.admin.domain.model

import androidx.annotation.Keep
import com.mtdevelopment.core.model.Order
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class OptimizedRouteWithOrders(
    val optimizedRoute: List<Pair<Double, Double>>,
    val optimizedOrders: List<Order>
)
