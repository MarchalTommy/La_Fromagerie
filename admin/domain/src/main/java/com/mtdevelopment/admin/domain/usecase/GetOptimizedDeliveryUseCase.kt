package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.GoogleRouteRepository

class GetOptimizedDeliveryUseCase(
    private val googleRouteRepository: GoogleRouteRepository
) {

    suspend operator fun invoke(
        addresses: List<String>
    ): List<Pair<Double, Double>>? {
        return googleRouteRepository.getOptimizedDeliveryPath(addresses)
    }

}