package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.CurrentLocation
import com.mtdevelopment.admin.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow

class GetCurrentLocationUseCase(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(): Flow<CurrentLocation> =
        locationRepository.getCurrentLocationUpdates()
}