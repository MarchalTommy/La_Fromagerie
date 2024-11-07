package com.mtdevelopment.checkout.presentation.composable

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mapbox.android.core.permissions.PermissionsManager.Companion.areLocationPermissionsGranted
import com.mtdevelopment.core.model.DeliveryPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

@Composable
fun PermissionManagerComposable(
    onUpdateUserCity: (String) -> Unit,
    onUpdateUserIsOnPath: (Boolean) -> Unit,
    onUpdateLocalisationState: (Boolean) -> Unit,
    onUpdateSelectedPath: (DeliveryPath) -> Unit,
    onUpdateUserCityLocation: (Pair<Double, Double>) -> Unit,
    onUpdateShouldShowLocalisationPermission: (Boolean) -> Unit,
    setIsLoading: (Boolean) -> Unit
) {

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    val context = LocalContext.current
    val geocoder = Geocoder(context)

    RequestLocationPermission(
        onPermissionGranted = {
            fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(context)
            getLastLocation(context = context,
                fusedLocationProviderClient = fusedLocationProviderClient,
                onGetLastLocationSuccess = { lastLocation ->
                    onUpdateLocalisationState.invoke(true)
                    onUpdateUserCityLocation.invoke(lastLocation)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(
                            lastLocation.first,
                            lastLocation.second,
                            1
                        ) { addressesList ->
                            getCityFromGeocoder(addressesList,
                                onUpdateUserIsOnPath = {
                                    onUpdateUserIsOnPath.invoke(it)
                                }, onUpdateUserCity = {
                                    onUpdateUserCity.invoke(it)
                                }, onUpdateSelectedPath = {
                                    onUpdateSelectedPath.invoke(it)
                                })
                        }
                    } else {
                        CoroutineScope(Dispatchers.IO).launch {
                            val addressesList = try {
                                // TODO: Loader + async ?
                                setIsLoading(true)
                                geocoder.getFromLocation(
                                    lastLocation.first,
                                    lastLocation.second,
                                    1
                                )
                            } catch (e: IOException) {
                                null
                            }
                            getCityFromGeocoder(
                                addressesList,
                                onUpdateUserIsOnPath = {
                                    onUpdateUserIsOnPath.invoke(it)
                                }, onUpdateUserCity = {
                                    onUpdateUserCity.invoke(it)
                                }, onUpdateSelectedPath = {
                                    onUpdateSelectedPath.invoke(it)
                                }
                            )

                        }.invokeOnCompletion {
                            setIsLoading(false)
                            onUpdateShouldShowLocalisationPermission.invoke(false)
                        }
                    }
                },
                onGetLastLocationFailed = {
                    onUpdateLocalisationState.invoke(false)
                    onUpdateUserCity.invoke("Unknown")
                })
        },
        onPermissionDenied = {
            onUpdateLocalisationState.invoke(false)
            onUpdateShouldShowLocalisationPermission.invoke(false)
        },
        onPermissionsRevoked = {
            onUpdateLocalisationState.invoke(false)
            onUpdateShouldShowLocalisationPermission.invoke(false)
        }
    )
}

fun getCityFromGeocoder(
    addressesList: List<Address>?,
    onUpdateUserIsOnPath: (Boolean) -> Unit,
    onUpdateSelectedPath: (DeliveryPath) -> Unit,
    onUpdateUserCity: (String) -> Unit
) {
    val foundAddress = addressesList?.find { address ->
        val correctPath =
            DeliveryPath.entries.find { path ->
                path.availableCities.contains(
                    address.locality
                )
            }
        if (correctPath != null) {
            onUpdateUserIsOnPath.invoke(true)
            onUpdateSelectedPath.invoke(correctPath)
            true
        } else {
            onUpdateUserIsOnPath.invoke(false)
            false
        }
    }
    foundAddress?.locality?.let { locality ->
        onUpdateUserCity.invoke(locality)
    }
}

@SuppressLint("MissingPermission")
fun getLastLocation(
    context: Context,
    fusedLocationProviderClient: FusedLocationProviderClient,
    onGetLastLocationSuccess: (Pair<Double, Double>) -> Unit,
    onGetLastLocationFailed: (Exception) -> Unit
) {
    if (areLocationPermissionsGranted(context)) {
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location ->
                location?.let {
                    onGetLastLocationSuccess(Pair(it.latitude, it.longitude))
                }
            }
            .addOnFailureListener { exception ->
                onGetLastLocationFailed(exception)
            }
    }
}