package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.model.OptimizedRouteWithOrders
import com.mtdevelopment.admin.domain.repository.AdminDatastorePreference
import com.mtdevelopment.admin.domain.repository.GoogleRouteRepository
import com.mtdevelopment.core.model.Order
import kotlinx.coroutines.flow.first

class GetOptimizedDeliveryUseCase(
    private val googleRouteRepository: GoogleRouteRepository,
    private val adminDatastorePreference: AdminDatastorePreference
) {

    // TODO: CLEANER MY DUDE
    suspend operator fun invoke(
        addresses: List<String>,
        dailyOrders: List<Order>
    ): OptimizedRouteWithOrders {

        return if (adminDatastorePreference.dailyDeliveryPathGeocodedFlow.first() == null) {
            googleRouteRepository.getOptimizedDeliveryPath(addresses, dailyOrders)
        } else {
            adminDatastorePreference.dailyDeliveryPathGeocodedFlow.first()!!
        }
    }

}