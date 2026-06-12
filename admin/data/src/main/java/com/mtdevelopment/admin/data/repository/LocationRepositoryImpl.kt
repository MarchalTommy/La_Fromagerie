package com.mtdevelopment.admin.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mtdevelopment.admin.domain.repository.CurrentLocation
import com.mtdevelopment.admin.domain.repository.LocationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Implementation of [LocationRepository] using Google Play Services Fused Location Provider.
 * This class provides methods to get continuous location updates as a [Flow] or a single location fix.
 */
class LocationRepositoryImpl(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(
        context
    )
) : LocationRepository {


    companion object {
        /**
         * Default interval for location updates in milliseconds.
         */
        const val DEFAULT_LOCATION_INTERVAL_MS = 15000L
    }

    /**
     * Creates a cold [Flow] that emits [CurrentLocation] updates.
     * The updates start when the flow is collected and stop when the collection is cancelled.
     */
    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocationUpdates(): Flow<CurrentLocation> = callbackFlow {
        val locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, DEFAULT_LOCATION_INTERVAL_MS)
                .setMinUpdateIntervalMillis(DEFAULT_LOCATION_INTERVAL_MS / 2) // Allow slightly faster if available
                .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    launch {
                        send(CurrentLocation(location.latitude, location.longitude))
                    }
                }
            }
        }

        // Request location updates
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // When the flow is closed, remove location updates automatically
        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    /**
     * Fetches the last known location from the Fused Location Provider.
     * If no cached location is available (e.g. right after a device reboot), falls back to
     * requesting a fresh single fix before giving up.
     */
    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocationOnce(): CurrentLocation? {
        val lastKnown = suspendCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    continuation.resume(
                        location?.let { CurrentLocation(it.latitude, it.longitude) }
                    )
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }
        return lastKnown ?: requestFreshLocation()
    }

    /**
     * Requests a single up-to-date location fix from the Fused Location Provider.
     */
    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(): CurrentLocation? {
        return suspendCoroutine { continuation ->
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                /* cancellationToken = */ null
            )
                .addOnSuccessListener { location ->
                    continuation.resume(
                        location?.let { CurrentLocation(it.latitude, it.longitude) }
                    )
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }
    }

    /**
     * Intentional no-op: continuous updates started by [getCurrentLocationUpdates] are
     * automatically removed when the flow collection is cancelled (see its awaitClose block),
     * so there is no out-of-band subscription to stop.
     */
    override suspend fun stopLocationUpdates() {
        // Nothing to do: cleanup is handled by awaitClose in getCurrentLocationUpdates.
    }
}