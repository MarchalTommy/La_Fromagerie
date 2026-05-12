package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.CurrentLocation
import com.mtdevelopment.admin.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to observe continuous location updates of the device.
 */
class GetCurrentLocationUseCase(
    private val locationRepository: LocationRepository
) {
    /**
     * Executes the use case.
     * @return A [Flow] of [CurrentLocation] emitting the latest coordinates.
     */
    suspend operator fun invoke(): Flow<CurrentLocation> =
        locationRepository.getCurrentLocationUpdates()
}