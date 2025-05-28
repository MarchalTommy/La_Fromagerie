package com.mtdevelopment.admin.domain.repository

interface GoogleRouteRepository {
    suspend fun getOptimizedDeliveryPath(addresses: List<String>): List<Pair<Double, Double>>?
}