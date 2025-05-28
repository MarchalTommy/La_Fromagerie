package com.mtdevelopment.admin.data.repository

import com.mtdevelopment.admin.data.model.GoogleOptimizedComputedRouteResponse
import com.mtdevelopment.admin.data.source.GoogleRouteDataSource
import com.mtdevelopment.admin.domain.repository.GoogleRouteRepository

class GoogleRouteRepositoryImpl(
    private val googleRouteDataSource: GoogleRouteDataSource
) : GoogleRouteRepository {

    override suspend fun getOptimizedDeliveryPath(addresses: List<String>): List<Pair<Double, Double>>? {

        val result = googleRouteDataSource.getOptimizedRoute(addressesList = addresses)
        val response = result.data as? GoogleOptimizedComputedRouteResponse

        return response?.routes?.firstOrNull()?.legs?.mapNotNull {
            Pair(
                it?.endLocation?.latLng?.latitude ?: 0.0,
                it?.endLocation?.latLng?.longitude ?: 0.0
            )
        }
    }

}