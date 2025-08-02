package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.CurrentLocation
import com.mtdevelopment.admin.domain.repository.LocationRepository

class GetCurrentLocationOnceUseCase(
    private val locationRepository: LocationRepository
) {

    suspend operator fun invoke(): CurrentLocation? {
        return locationRepository.getCurrentLocationOnce()
    }

}