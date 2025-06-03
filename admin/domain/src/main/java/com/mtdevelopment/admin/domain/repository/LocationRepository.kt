package com.mtdevelopment.admin.domain.repository

import kotlinx.coroutines.flow.Flow

data class CurrentLocation(val latitude: Double, val longitude: Double)

interface LocationRepository {

    suspend fun getCurrentLocationUpdates(): Flow<CurrentLocation>

    suspend fun stopLocationUpdates()

}