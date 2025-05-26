package com.mtdevelopment.admin.domain.repository

interface GoogleRouteRepository {
    fun getOptimizedDeliveryPath(addresses: List<String>): List<String>
}