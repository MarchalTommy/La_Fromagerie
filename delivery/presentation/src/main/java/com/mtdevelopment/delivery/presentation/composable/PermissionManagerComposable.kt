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
import com.mtdevelopment.delivery.domain.usecase.DeliveryEligibility
import com.mtdevelopment.delivery.domain.usecase.DetermineDeliveryEligibilityUseCase
import com.mtdevelopment.delivery.presentation.model.UiDeliveryPath
import com.mtdevelopment.delivery.presentation.model.toDomainDeliveryPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


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
 * Resolves a GPS coordinate into a city, street and address line, then hands the decision to
 * [DetermineDeliveryEligibilityUseCase].
 *
 * Only the geocoding lives here; the path-matching rules are in the use case, shared with the
 * typed-address flow in `CustomerContent` and covered by unit tests.
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

        // 2. Match against the delivery paths
        val result = DetermineDeliveryEligibilityUseCase().invoke(
            paths = allPaths.map { it.toDomainDeliveryPath() },
            userCity = userCity,
            userStreet = userStreet,
            addressText = userAddress,
            userLocation = userLocation
        )

        withContext(Dispatchers.Main) {
            onResult(
                result.eligibility,
                result.resolvedCity,
                userAddress,
                result.matchingPath?.let { matched -> allPaths.find { it.id == matched.id } }
            )
        }
    }
}