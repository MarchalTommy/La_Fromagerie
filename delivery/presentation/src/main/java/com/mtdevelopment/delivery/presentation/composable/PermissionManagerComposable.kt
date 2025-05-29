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


// Constante pour la distance maximale en mètres (5km à vol d'oiseau)
const val MAX_DISTANCE_FOR_PICKUP_METERS = 5000.0

// Enum pour représenter le statut de livraison/prise en charge
enum class DeliveryEligibility {
    DELIVERABLE, // Livraison possible
    ASK_FOR_SUPPORT, // Trop loin, faire une demande
    NOT_ELIGIBLE // Ni livraison, ni prise en charge
}


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

            // Permission accordée : récupérer la localisation
            setIsLoading(true)
            getLastLocation(
                context = context,
                fusedLocationProviderClient = fusedLocationProviderClient,
                onSuccess = { userLocation ->
                    onUpdateUserLocation(userLocation)
                    onUpdateLocalisationState.invoke(true)
                    // Lancer la vérification de la ville et de la proximité dans une coroutine
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
                    // Échec de récupération de la localisation
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
        val isNearPathCity: Boolean
        var closestDistance = Double.MAX_VALUE
        var matchingPathForCity: UiDeliveryPath? = null

        // 1. Géocodage pour obtenir le nom de la ville
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
        } catch (e: IOException) {
            userCity = null // ou "Erreur Geocoder"
        } catch (e: IllegalArgumentException) {
            userCity = null // ou "Erreur Coordonnées"
        }


        // 2. Vérifier la proximité et si la ville est dans un parcours
        for (path in allPaths) {
            for (cityInfo in path.cities) {
                val cityName = cityInfo.first
                val cityLocation = path.locations!![path.cities.indexOf(cityInfo)]

                // Calculer la distance
                val distance = calculateDistance(
                    userLocation.first, userLocation.second,
                    cityLocation.first, cityLocation.second
                )

                if (distance < closestDistance) {
                    closestDistance = distance.toDouble()
                }

                // Vérifier si la ville géocodée correspond à une ville du parcours
                if (userCity != null && userCity.equals(cityName, ignoreCase = true)) {
                    matchingPathForCity = path
                }
            }
        }

        isNearPathCity = closestDistance <= MAX_DISTANCE_FOR_PICKUP_METERS

        // 3. Déterminer l'éligibilité
        val eligibility = when {
            matchingPathForCity != null -> DeliveryEligibility.DELIVERABLE // Priorité si livrable
            isNearPathCity -> DeliveryEligibility.ASK_FOR_SUPPORT // Ensuite si trop loin
            else -> DeliveryEligibility.NOT_ELIGIBLE // Ensuite si vraiment trop loin
        }

        // Retourner le résultat sur le thread principal si nécessaire pour l'UI
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