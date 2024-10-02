package com.mtdevelopment.checkout.presentation.screen

import android.annotation.SuppressLint
import android.location.Geocoder
import android.location.Geocoder.GeocodeListener
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mapbox.android.core.permissions.PermissionsManager.Companion.areLocationPermissionsGranted
import com.mapbox.common.MapboxOptions
import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.checkout.presentation.BuildConfig.MAPBOX_PUBLIC_TOKEN
import com.mtdevelopment.checkout.presentation.composable.DatePickerComposable
import com.mtdevelopment.checkout.presentation.composable.DateTextField
import com.mtdevelopment.checkout.presentation.composable.DeliveryPathPickerComposable
import com.mtdevelopment.checkout.presentation.composable.LocalisationTextComposable
import com.mtdevelopment.checkout.presentation.composable.LocalisationTypePicker
import com.mtdevelopment.checkout.presentation.composable.MapBoxComposable
import com.mtdevelopment.checkout.presentation.composable.RequestLocationPermission
import com.mtdevelopment.checkout.presentation.composable.getDatePickerState
import com.mtdevelopment.checkout.presentation.model.DeliveryPath
import com.mtdevelopment.checkout.presentation.model.ShippingDefaultSelectableDates
import com.mtdevelopment.checkout.presentation.model.ShippingSelectableMetaDates
import com.mtdevelopment.checkout.presentation.model.ShippingSelectablePontarlierDates
import com.mtdevelopment.checkout.presentation.model.ShippingSelectableSalinDates
import com.mtdevelopment.checkout.presentation.viewmodel.CheckoutViewModel
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun DeliveryOptionScreen(
    cartViewModel: CartViewModel? = null,
    checkoutViewModel: CheckoutViewModel? = null,
    navigateToHome: () -> Unit = {},
    screenSize: ScreenSize = rememberScreenSize()
) {

    val context = LocalContext.current

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    fun getLastLocation(
        onGetLastLocationSuccess: (Pair<Double, Double>) -> Unit,
        onGetLastLocationFailed: (Exception) -> Unit
    ) {
        // Check if location permissions are granted
        if (areLocationPermissionsGranted(context)) {
            // Retrieve the last known location
            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        // If location is not null, invoke the success callback with latitude and longitude
                        onGetLastLocationSuccess(Pair(it.latitude, it.longitude))
                    }
                }
                .addOnFailureListener { exception ->
                    // If an error occurs, invoke the failure callback with the exception
                    onGetLastLocationFailed(exception)
                }
        }
    }

    if (MapboxOptions.accessToken != MAPBOX_PUBLIC_TOKEN) {
        MapboxOptions.accessToken = MAPBOX_PUBLIC_TOKEN
    }

    val selectedPath = checkoutViewModel?.selectedPath?.collectAsState()

    val datePickerState =
        when (selectedPath?.value) {

            DeliveryPath.PATH_META -> {
                getDatePickerState(ShippingSelectableMetaDates())
            }

            DeliveryPath.PATH_SALIN -> {
                getDatePickerState(ShippingSelectableSalinDates())
            }

            DeliveryPath.PATH_PON -> {
                getDatePickerState(ShippingSelectablePontarlierDates())
            }

            else -> {
                getDatePickerState(ShippingDefaultSelectableDates())
            }
        }

    val datePickerVisibility = remember { mutableStateOf(false) }
    val dateFieldText = remember { mutableStateOf("") }

    val localisationPermissionState = remember { mutableStateOf(false) }

    val localisationSuccess = remember { mutableStateOf(false) }
    val geolocIsOnPath = remember { mutableStateOf(false) }

    val showDeliveryPathPicker = remember { mutableStateOf(false) }
    fun showDeliverySelection() {
        showDeliveryPathPicker.value = true
    }

    LaunchedEffect(selectedPath?.value) {
        dateFieldText.value = ""
    }

    val userCity = remember { mutableStateOf("") }
    val userCityLocation = remember { mutableStateOf<Pair<Double, Double>?>(null) }

    fun getCityFromGeocoder(addressesList: List<android.location.Address>?) {
        val foundAddress = addressesList?.find { address ->
            val correctPath =
                DeliveryPath.entries.find { path ->
                    path.availableCities.contains(
                        address.locality
                    )
                }
            if (correctPath != null) {
                geolocIsOnPath.value = true
                checkoutViewModel?.setSelectedPath(correctPath)
                true
            } else {
                geolocIsOnPath.value = false
                false
            }
        }
        foundAddress?.locality?.let { locality ->
            userCity.value = locality
        }
    }

    val geocodeListener = GeocodeListener { addressesList ->
        getCityFromGeocoder(addressesList)
    }

    Surface(modifier = Modifier.fillMaxSize()) {

        // Localisation permission
        if (localisationPermissionState.value) {
            RequestLocationPermission(
                onPermissionGranted = {
                    fusedLocationProviderClient =
                        LocationServices.getFusedLocationProviderClient(context)
                    getLastLocation(onGetLastLocationSuccess = {
                        localisationSuccess.value = true
                        userCityLocation.value = it
                        val geocoder = Geocoder(context)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            geocoder.getFromLocation(
                                it.first,
                                it.second,
                                1,
                                geocodeListener
                            )
                        } else {
                            val addressesList = geocoder.getFromLocation(it.first, it.second, 1)
                            getCityFromGeocoder(addressesList)
                        }

                    },
                        onGetLastLocationFailed = {
                            userCity.value = "Unknown"
                        })
                },
                onPermissionDenied = {
                    localisationSuccess.value = false
                    localisationPermissionState.value = false
                },
                onPermissionsRevoked = {
                    localisationSuccess.value = false
                    localisationPermissionState.value = false
                }
            )
        }

        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {

            // Map Card
            MapBoxComposable(userLocation = userCityLocation)

            // Localisation Type Picker
            LocalisationTypePicker(
                localisationPermissionState = localisationPermissionState,
                showDeliverySelection = { showDeliverySelection() },
                selectedPath = selectedPath,
                localisationSuccess = localisationSuccess
            )

            if (localisationSuccess.value || selectedPath?.value != null || geolocIsOnPath.value) {
                LocalisationTextComposable(
                    selectedPath = selectedPath,
                    geolocIsOnPath = geolocIsOnPath,
                    userCity = userCity
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(8.dp),
                thickness = Dp.Hairline
            )

            DateTextField(
                datePickerVisibility = datePickerVisibility,
                dateFieldText = dateFieldText,
                datePickerState = datePickerState
            )

            if (datePickerVisibility.value) {
                DatePickerComposable(
                    datePickerVisibility = datePickerVisibility,
                    dateFieldText = dateFieldText,
                    datePickerState = datePickerState
                )
            }
        }

        if (showDeliveryPathPicker.value) {
            if (checkoutViewModel != null) {
                DeliveryPathPickerComposable(checkoutViewModel) {
                    showDeliveryPathPicker.value = false
                }
            }
        }
    }

}