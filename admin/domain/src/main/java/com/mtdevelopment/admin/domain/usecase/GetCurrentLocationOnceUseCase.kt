package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.CurrentLocation
import com.mtdevelopment.admin.domain.repository.LocationRepository

/**
 * Use case to retrieve the current device location a single time.
 */
class GetCurrentLocationOnceUseCase(
    private val locationRepository: LocationRepository
) {

    /**
     * Executes the use case.
     * @return The [CurrentLocation] if available, or null.
     */
    suspend operator fun invoke(): CurrentLocation? {
        return locationRepository.getCurrentLocationOnce()
    }

}