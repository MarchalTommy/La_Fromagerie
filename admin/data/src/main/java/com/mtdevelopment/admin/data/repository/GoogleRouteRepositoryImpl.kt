package com.mtdevelopment.admin.data.repository

import com.mtdevelopment.admin.data.source.GoogleRouteDataSource
import com.mtdevelopment.admin.domain.model.OptimizedRouteWithOrders
import com.mtdevelopment.admin.domain.repository.GoogleRouteRepository
import com.mtdevelopment.core.domain.reorderList
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.util.NetWorkResult

class GoogleRouteRepositoryImpl(
    private val googleRouteDataSource: GoogleRouteDataSource
) : GoogleRouteRepository {

    override suspend fun getOptimizedDeliveryPath(
        addresses: List<String>,
        dailyOrders: List<Order>
    ): OptimizedRouteWithOrders {

        val result = googleRouteDataSource.getOptimizedRoute(addressesList = addresses)
        val response = (result as? NetWorkResult.Success)?.data

        val indexes =
            response?.routes?.firstOrNull()?.optimizedIntermediateWaypointIndex ?: listOf()

        val finalItem = OptimizedRouteWithOrders(
            optimizedRoute = response?.routes?.firstOrNull()?.legs?.mapNotNull {
                Pair(
                    it?.endLocation?.latLng?.latitude ?: 0.0,
                    it?.endLocation?.latLng?.longitude ?: 0.0
                )
            } ?: listOf(),
            optimizedOrders = reorderList(dailyOrders, indexes)
        )


        return finalItem
    }

}