package com.mtdevelopment.checkout.presentation.screen

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavArgs
import app.rive.runtime.kotlin.core.Rive
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mapbox.android.core.permissions.PermissionsManager.Companion.areLocationPermissionsGranted
import com.mapbox.common.MapboxOptions
import com.mtdevelopment.checkout.presentation.BuildConfig.MAPBOX_PUBLIC_TOKEN
import com.mtdevelopment.checkout.presentation.R
import com.mtdevelopment.checkout.presentation.composable.DatePickerComposable
import com.mtdevelopment.checkout.presentation.composable.DateTextField
import com.mtdevelopment.checkout.presentation.composable.DeliveryPathPickerComposable
import com.mtdevelopment.checkout.presentation.composable.LocalisationTextComposable
import com.mtdevelopment.checkout.presentation.composable.LocalisationTypePicker
import com.mtdevelopment.checkout.presentation.composable.MapBoxComposable
import com.mtdevelopment.checkout.presentation.composable.RequestLocationPermission
import com.mtdevelopment.checkout.presentation.composable.UserInfoComposable
import com.mtdevelopment.checkout.presentation.composable.getDatePickerState
import com.mtdevelopment.checkout.presentation.model.DeliveryPath
import com.mtdevelopment.checkout.presentation.model.ShippingDefaultSelectableDates
import com.mtdevelopment.checkout.presentation.model.ShippingSelectableMetaDates
import com.mtdevelopment.checkout.presentation.model.ShippingSelectablePontarlierDates
import com.mtdevelopment.checkout.presentation.model.ShippingSelectableSalinDates
import com.mtdevelopment.checkout.presentation.model.UiCheckoutObject
import com.mtdevelopment.checkout.presentation.model.UserInfo
import com.mtdevelopment.checkout.presentation.viewmodel.DeliveryViewModel
import com.mtdevelopment.core.presentation.composable.RiveAnimation
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.IOException

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun DeliveryOptionScreen(
    screenSize: ScreenSize = rememberScreenSize(),
    navigateToHome: () -> Unit = {},
    navigateToCheckout: (UiCheckoutObject) -> Unit = {}
) {

    // TODO: FIX AUTO-GEOLOC ->
    // Double click for it to geoloc
    // Never seems to work on emulator ?! What about real world then...
    // TODO: FIX LOADER ->
    // Is still blocked by UI thread breaking when Geocoder bug. Place geocoder in custom thread ?
    // Removes itself just after the delay, not after boolean set to false OR delay. Bad brain of me.

    val deliveryViewModel = koinViewModel<DeliveryViewModel>()

    val context = LocalContext.current

    val geocoder = Geocoder(context)

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    val scrollState = rememberScrollState()
    val columnScrollingEnabled = remember { mutableStateOf(true) }

    val isLoading = remember { mutableStateOf(false) }
    val shouldShowLoading = remember { mutableStateOf(false) }

    fun getLastLocation(
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

    if (MapboxOptions.accessToken != MAPBOX_PUBLIC_TOKEN) {
        MapboxOptions.accessToken = MAPBOX_PUBLIC_TOKEN
    }

    val selectedPath = deliveryViewModel?.selectedPath?.collectAsState()

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

    val shouldDatePickerBeClickable = remember { mutableStateOf(false) }
    val datePickerVisibility = remember { mutableStateOf(false) }
    val dateFieldText = remember { mutableStateOf("") }
    val userNameFieldText = remember { mutableStateOf("") }
    val userAddressFieldText = remember { mutableStateOf("") }

    val localisationPermissionState = remember { mutableStateOf(false) }

    val localisationSuccess = remember { mutableStateOf(false) }
    val geolocIsOnPath = remember { mutableStateOf(false) }

    val showDeliveryPathPicker = remember { mutableStateOf(false) }
    fun showDeliverySelection() {
        showDeliveryPathPicker.value = true
    }

    LaunchedEffect(Unit) {
        Rive.init(context)
    }

    LaunchedEffect(selectedPath?.value) {
        shouldDatePickerBeClickable.value = selectedPath?.value != null
        dateFieldText.value = ""
    }

    val userCity = remember { mutableStateOf("") }
    val userCityLocation = remember { mutableStateOf<Pair<Double, Double>?>(null) }

    fun getCityFromGeocoder(addressesList: List<Address>?) {
        val foundAddress = addressesList?.find { address ->
            val correctPath =
                DeliveryPath.entries.find { path ->
                    path.availableCities.contains(
                        address.locality
                    )
                }
            if (correctPath != null) {
                geolocIsOnPath.value = true
                deliveryViewModel?.setSelectedPath(correctPath)
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

    Surface(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(state = scrollState, enabled = columnScrollingEnabled.value)
    ) {
        // Localisation permission
        if (localisationPermissionState.value) {
            RequestLocationPermission(
                onPermissionGranted = {
                    fusedLocationProviderClient =
                        LocationServices.getFusedLocationProviderClient(context)
                    getLastLocation(onGetLastLocationSuccess = {
                        localisationSuccess.value = true
                        userCityLocation.value = it
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            geocoder.getFromLocation(
                                it.first,
                                it.second,
                                1
                            ) { addressesList ->
                                getCityFromGeocoder(addressesList)
                            }
                        } else {
                            val addressesList =
                                try {
                                    // TODO: Loader + async ?
                                    geocoder.getFromLocation(it.first, it.second, 1)
                                } catch (e: IOException) {
                                    null
                                }
                            getCityFromGeocoder(addressesList)
                        }
                    },
                        onGetLastLocationFailed = {
                            localisationSuccess.value = false
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
            MapBoxComposable(
                userLocation = userCityLocation,
                chosenPath = selectedPath,
                columnScrollingEnabled = columnScrollingEnabled,
                isLoading = isLoading
            )

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
                shouldBeClickable = shouldDatePickerBeClickable,
                datePickerVisibility = datePickerVisibility,
                dateFieldText = dateFieldText,
                datePickerState = datePickerState
            )

            UserInfoComposable(
                userNameFieldText,
                "Nom complet"
            ) {
                Icon(Icons.Rounded.Person, "")
            }

            UserInfoComposable(
                userAddressFieldText,
                "Adresse exacte"
            ) {
                Icon(Icons.Rounded.Place, "")
            }

            Button(modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp),
                contentPadding = PaddingValues(16.dp),
                border = BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.secondary
                ),
                colors = ButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.secondary,
                    disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    disabledContentColor = MaterialTheme.colorScheme.secondary
                ),
                elevation = ButtonDefaults.elevatedButtonElevation(),
                shape = RoundedCornerShape(8.dp),
                onClick = {
                    deliveryViewModel?.setUserInfo(
                        UserInfo(
                            userNameFieldText.value,
                            userAddressFieldText.value
                        )
                    )
//                    navigateToCheckout.invoke(
//                        UiCheckoutObject(
//                            cartItems = ,
//                            userInfo = ,
//                            deliveryPath = ,
//                            deliveryDate =
//                        )
//                    )
                }) {
                Text("Valider et passer au paiement")
            }
        }

        LaunchedEffect(isLoading.value) {
            val currentValue = isLoading.value
            val showUpDelay = 200L
            val removeDelay = 1000L
            CoroutineScope(Dispatchers.Default).launch {
                delay(if (isLoading.value) showUpDelay else removeDelay)
                // Should show loading only if state hasn't change in last 200ms
                if (isLoading.value == currentValue && isLoading.value) {
                    Log.i(TAG, "DeliveryOptionScreen: SHOWING LOADER")
                    shouldShowLoading.value = true
                } else if (isLoading.value == currentValue && !isLoading.value) {
                    Log.i(TAG, "DeliveryOptionScreen: REMOVING LOADER")
                    shouldShowLoading.value = false
                }
            }
        }

        if (shouldShowLoading.value) {
            RiveAnimation(
                modifier = Modifier.fillMaxSize(),
                resId = R.raw.goat_loading,
                contentDescription = "Loading animation"
            )
        }

        if (datePickerVisibility.value) {
            DatePickerComposable(
                datePickerVisibility = datePickerVisibility,
                dateFieldText = dateFieldText,
                datePickerState = datePickerState
            )
        }

        if (showDeliveryPathPicker.value) {
            DeliveryPathPickerComposable(deliveryViewModel) {
                showDeliveryPathPicker.value = false
            }
        }
    }
}