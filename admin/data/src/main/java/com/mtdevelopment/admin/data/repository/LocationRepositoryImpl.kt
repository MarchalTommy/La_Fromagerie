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

class LocationRepositoryImpl(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(
        context
    )
) : LocationRepository {


    companion object {
        const val DEFAULT_LOCATION_INTERVAL_MS = 15000L
    }

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

        // When the flow is closed, remove location updates
        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocationOnce(): CurrentLocation? {
        return suspendCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(CurrentLocation(location.latitude, location.longitude))
                    }
                }
        }
    }

    override suspend fun stopLocationUpdates() {
        TODO("Not yet implemented")
    }
}