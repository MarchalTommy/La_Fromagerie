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
     *
     * The cached route is only reused when it was computed for the same set of orders:
     * if the day's orders changed since the cache was written (or [forceRefresh] is true),
     * a fresh optimized route is fetched instead of serving a stale one.
     *
     * @param addresses The list of delivery addresses.
     * @param dailyOrders The list of orders for the day.
     * @param forceRefresh If true, bypasses the cached route entirely.
     * @return The [OptimizedRouteWithOrders] representing the best route.
     */
    suspend operator fun invoke(
        addresses: List<String>,
        dailyOrders: List<Order>,
        forceRefresh: Boolean = false
    ): OptimizedRouteWithOrders {

        val cached = adminDatastorePreference.dailyDeliveryPathGeocodedFlow.first()
        val cacheMatchesOrders =
            cached?.optimizedOrders?.map { it.id }?.toSet() == dailyOrders.map { it.id }.toSet()

        return if (forceRefresh || cached == null || !cacheMatchesOrders) {
            googleRouteRepository.getOptimizedDeliveryPath(addresses, dailyOrders)
        } else {
            cached
        }
    }

}