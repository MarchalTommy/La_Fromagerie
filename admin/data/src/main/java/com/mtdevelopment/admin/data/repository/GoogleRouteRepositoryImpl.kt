package com.mtdevelopment.admin.data.repository

import com.mtdevelopment.admin.data.source.GoogleRouteDataSource
import com.mtdevelopment.admin.domain.repository.GoogleRouteRepository

class GoogleRouteRepositoryImpl(
    private val googleRouteDataSource: GoogleRouteDataSource
) : GoogleRouteRepository {
    override fun getOptimizedDeliveryPath(addresses: List<String>): List<String> {
        TODO("Not yet implemented")
    }

}