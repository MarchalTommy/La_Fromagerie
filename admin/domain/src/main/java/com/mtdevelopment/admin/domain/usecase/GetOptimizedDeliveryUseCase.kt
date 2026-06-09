package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.model.OptimizedRouteWithOrders
import com.mtdevelopment.admin.domain.repository.AdminDatastorePreference
import com.mtdevelopment.admin.domain.repository.GoogleRouteRepository
import com.mtdevelopment.core.model.Order
import kotlinx.coroutines.flow.first

/**
 * Use case to retrieve the optimized delivery path for a set of addresses and orders.
 * It first checks if an optimized path is already cached in [AdminDatastorePreference].
 * If not, it fetches a new optimized path from [GoogleRouteRepository].
 */
class GetOptimizedDeliveryUseCase(
    private val googleRouteRepository: GoogleRouteRepository,
    private val adminDatastorePreference: AdminDatastorePreference
) {

    /**
     * Executes the use case.
     * @param addresses The list of delivery addresses.
     * @param dailyOrders The list of orders for the day.
     * @return The [OptimizedRouteWithOrders] representing the best route.
     * // TODO: Consider adding a forced refresh mechanism if the input addresses/orders change but a cached version exists.
     */
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