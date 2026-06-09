package com.mtdevelopment.admin.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Represents the current geographical coordinates.
 * @property latitude The latitude in degrees.
 * @property longitude The longitude in degrees.
 */
data class CurrentLocation(val latitude: Double, val longitude: Double)

/**
 * Repository interface for accessing the device's current location.
 * Provides both continuous updates and single-shot location requests.
 */
interface LocationRepository {

    /**
     * Starts and returns a flow of location updates.
     * @return A Flow of [CurrentLocation] objects.
     */
    suspend fun getCurrentLocationUpdates(): Flow<CurrentLocation>

    /**
     * Stops any ongoing location updates.
     */
    suspend fun stopLocationUpdates()

    /**
     * Retrieves the current location once.
     * @return The [CurrentLocation] if available, otherwise null.
     */
    suspend fun getCurrentLocationOnce(): CurrentLocation?
}