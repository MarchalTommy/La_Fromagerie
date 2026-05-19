package com.mtdevelopment.delivery.presentation.composable

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mapbox.android.core.permissions.PermissionsManager.Companion.areLocationPermissionsGranted
import com.mtdevelopment.core.domain.calculateDistance
import com.mtdevelopment.delivery.presentation.model.UiDeliveryPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * Maximum distance (in meters) to allow a user to ask for manual support/delivery 
 * if they are not in an exact delivery zone.
 */
const val MAX_DISTANCE_FOR_PICKUP_METERS = 5000.0

/**
 * Enumeration representing the user's eligibility for delivery based on their location.
 */
enum class DeliveryEligibility {
    /** Delivery is officially supported for this exact location/city. */
    DELIVERABLE,

    /** User is close to a delivery zone and can request manual support. */
    ASK_FOR_SUPPORT,

    /** User is too far from any delivery path. */
    NOT_ELIGIBLE 
}


/**
 * A logical (non-visual) Composable that manages the GPS permission flow and determines 
 * delivery eligibility based on the user's current coordinates.
 * 
 * Logic flow:
 * 1. Requests location permission.
 * 2. Fetches the last known GPS coordinates.
 * 3. Uses Android's Geocoder to resolve coordinates into a city and address.
 * 4. Compares the user's location with defined delivery paths and zones.
 * 5. Updates the UI state with eligibility results.
 */
@Composable
fun PermissionManagerComposable(
    allPaths: List<UiDeliveryPath>,
    onUpdateEligibility: (eligibility: DeliveryEligibility, city: String?, userAddress: String?, selectedPath: UiDeliveryPath?) -> Unit,
    onUpdateUserLocation: (Pair<Double, Double>?) -> Unit,
    setIsLoading: (Boolean) -> Unit,
    onUpdateLocalisationState: (Boolean) -> Unit,
    onUpdateShouldShowLocalisationPermission: (Boolean) -> Unit,
) {

    val context = LocalContext.current
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    val coroutineScope = rememberCoroutineScope()

    RequestLocationPermission(
        onPermissionGranted = {
            fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(context)

            // Permission granted: start acquisition
            setIsLoading(true)
            getLastLocation(
                context = context,
                fusedLocationProviderClient = fusedLocationProviderClient,
                onSuccess = { userLocation ->
                    onUpdateUserLocation(userLocation)
                    onUpdateLocalisationState.invoke(true)

                    // Background check for city and path proximity
                    coroutineScope.launch {
                        checkLocationEligibility(
                            context = context,
                            userLocation = userLocation,
                            allPaths = allPaths,
                            onResult = { eligibility, city, userAddress, path ->
                                onUpdateEligibility(eligibility, city, userAddress, path)
                                setIsLoading(false)
                            }
                        )
                    }
                },
                onFailure = {
                    onUpdateUserLocation(null)
                    onUpdateEligibility(DeliveryEligibility.NOT_ELIGIBLE, "Unknown", null, null)
                    onUpdateLocalisationState.invoke(false)
                    setIsLoading(false)
                }
            )
        },
        onPermissionDenied = {
            onUpdateLocalisationState.invoke(false)
            onUpdateShouldShowLocalisationPermission.invoke(false)
        }
    )
}

/**
 * Fetches the device's last known location.
 */
@SuppressLint("MissingPermission")
fun getLastLocation(
    context: Context,
    fusedLocationProviderClient: FusedLocationProviderClient,
    onSuccess: (Pair<Double, Double>) -> Unit,
    onFailure: (Exception) -> Unit
) {
    if (areLocationPermissionsGranted(context)) {
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location ->
                location?.let {
                    onSuccess(Pair(it.latitude, it.longitude))
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}

/**
 * Core business logic for matching a GPS coordinate to a delivery path.
 * 
 * Algorithm:
 * 1. Geocode Lat/Lng to get City, Street, and Full Address.
 * 2. Iterate through all delivery paths:
 *    - Calculate distance to the center of each city in the path.
 *    - Find the minimum distance to any point on any path.
 *    - Filter paths that explicitly include the user's city name.
 * 3. Resolve the best matching path:
 *    - Priority 1: Exact street match within a path covering the user's city.
 *    - Priority 2: Generic path for the city (no street restrictions).
 * 4. Assign eligibility:
 *    - DELIVERABLE if a matching path is found.
 *    - ASK_FOR_SUPPORT if the user is within 5km of a path center.
 *    - NOT_ELIGIBLE otherwise.
 */
private suspend fun checkLocationEligibility(
    context: Context,
    userLocation: Pair<Double, Double>,
    allPaths: List<UiDeliveryPath>,
    onResult: (eligibility: DeliveryEligibility, city: String?, userAddress: String?, selectedPath: UiDeliveryPath?) -> Unit
) {
    withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context)
        var userCity: String?
        var userAddress: String? = null
        var userStreet: String? = null
        val isNearPathCity: Boolean
        var closestDistance = Double.MAX_VALUE
        var matchingPathForCity: UiDeliveryPath? = null

        // 1. Geocoding
        try {
            val addresses: List<Address>? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCoroutine { continuation ->
                        geocoder.getFromLocation(
                            userLocation.first,
                            userLocation.second,
                            1
                        ) { addressList ->
                            continuation.resume(addressList)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(userLocation.first, userLocation.second, 1)
                }
            userCity = addresses?.firstOrNull()?.locality
            userAddress = addresses?.firstOrNull()?.getAddressLine(0)
            userStreet = addresses?.firstOrNull()?.thoroughfare
        } catch (e: IOException) {
            userCity = null 
        } catch (e: IllegalArgumentException) {
            userCity = null 
        }


        // 2. Proximity and path analysis
        val pathsInCity = mutableListOf<UiDeliveryPath>()

        for (path in allPaths) {
            for (cityInfo in path.cities) {
                val cityName = cityInfo.first
                // Check safety of locations access
                val cityIndex = path.cities.indexOf(cityInfo)
                if (path.locations != null && path.locations.size > cityIndex) {
                    val cityLocation = path.locations[cityIndex]

                    // Calculate straight-line distance
                    val distance = calculateDistance(
                        userLocation.first, userLocation.second,
                        cityLocation.first, cityLocation.second
                    )

                    if (distance < closestDistance) {
                        closestDistance = distance.toDouble()
                    }
                }
            }

            // Path covers user's city?
            if (userCity != null && path.cities.any { it.first.equals(userCity, ignoreCase = true) }) {
                pathsInCity.add(path)
            }
        }

        // 3. Granular matching (Street level)
        if (pathsInCity.isNotEmpty()) {
            // Try exact street match
             if (userStreet != null) {
                 matchingPathForCity = pathsInCity.find { path ->
                     path.streets.any { it.equals(userStreet, ignoreCase = true) }
                 }
             }

            // If no street match, look for generic path (no streets defined = whole city covered)
             if (matchingPathForCity == null) {
                 matchingPathForCity = pathsInCity.find { it.streets.isEmpty() }
             }
        }

        isNearPathCity = closestDistance <= MAX_DISTANCE_FOR_PICKUP_METERS

        // 4. Determine final eligibility
        val eligibility = when {
            matchingPathForCity != null -> DeliveryEligibility.DELIVERABLE
            isNearPathCity -> DeliveryEligibility.ASK_FOR_SUPPORT
            else -> DeliveryEligibility.NOT_ELIGIBLE 
        }

        withContext(Dispatchers.Main) {
            onResult(
                eligibility,
                userCity,
                userAddress,
                if (eligibility == DeliveryEligibility.DELIVERABLE) matchingPathForCity else null
            )
        }
    }
}