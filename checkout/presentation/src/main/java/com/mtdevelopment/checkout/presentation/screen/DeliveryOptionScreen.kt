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
import com.mtdevelopment.checkout.presentation.model.ShippingDefaultSelectableDates
import com.mtdevelopment.checkout.presentation.model.ShippingSelectableMetaDates
import com.mtdevelopment.checkout.presentation.model.ShippingSelectablePontarlierDates
import com.mtdevelopment.checkout.presentation.model.ShippingSelectableSalinDates
import com.mtdevelopment.checkout.presentation.model.UserInfo
import com.mtdevelopment.checkout.presentation.viewmodel.DeliveryViewModel
import com.mtdevelopment.core.model.DeliveryPath
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
    navigateToCheckout: () -> Unit = {}
) {

    // TODO: FIX AUTO-GEOLOC ->
    // Double click for it to geoloc
    // TODO: FIX LOADER ->
    // Is still blocked by UI thread breaking when Geocoder bug. Place geocoder in custom thread ?
    // Removes itself just after the delay, not after boolean set to false OR delay. Bad brain of me.

    val deliveryViewModel = koinViewModel<DeliveryViewModel>()
    val context = LocalContext.current
    val geocoder = Geocoder(context)

    val state = deliveryViewModel.deliveryUiDataState

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

    LaunchedEffect(Unit) {
        Rive.init(context)
    }

    LaunchedEffect(selectedPath?.value) {
        deliveryViewModel.setIsDatePickerClickable(selectedPath?.value != null)
        deliveryViewModel.setDateFieldText("")
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
                deliveryViewModel.updateUserLocationOnPath(true)
                deliveryViewModel.setSelectedPath(correctPath)
                true
            } else {
                deliveryViewModel.updateUserLocationOnPath(false)
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
        if (state.shouldShowLocalisationPermission) {
            RequestLocationPermission(
                onPermissionGranted = {
                    fusedLocationProviderClient =
                        LocationServices.getFusedLocationProviderClient(context)
                    getLastLocation(onGetLastLocationSuccess = {
                        deliveryViewModel.updateLocalisationState(true)
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
                            deliveryViewModel.updateLocalisationState(false)
                            userCity.value = "Unknown"
                        })
                },
                onPermissionDenied = {
                    deliveryViewModel.updateLocalisationState(false)
                    deliveryViewModel.updateShouldShowLocalisationPermission(false)
                },
                onPermissionsRevoked = {
                    deliveryViewModel.updateLocalisationState(false)
                    deliveryViewModel.updateShouldShowLocalisationPermission(false)
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
                showDeliverySelection = {
                    deliveryViewModel.updateShowDeliveryPathPicker(true)
                },
                selectedPath = selectedPath,
                localisationSuccess = state.localisationSuccess,
                shouldAskLocalisationPermission = {
                    deliveryViewModel.updateShouldShowLocalisationPermission(true)
                }
            )

            if (state.localisationSuccess || selectedPath?.value != null || state.userLocationOnPath) {
                LocalisationTextComposable(
                    selectedPath = selectedPath,
                    geolocIsOnPath = state.userLocationOnPath,
                    userCity = userCity
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(8.dp),
                thickness = Dp.Hairline
            )

            DateTextField(
                shouldBeClickable = state.shouldDatePickerBeClickable,
                dateFieldText = state.dateFieldText,
                datePickerState = datePickerState,
                shouldShowDatePicker = {
                    deliveryViewModel.setIsDatePickerShown(true)
                },
                newDateFieldText = {
                    deliveryViewModel.setDateFieldText(it)
                }
            )

            UserInfoComposable(
                state.userNameFieldText,
                "Nom complet",
                updateText = {
                    deliveryViewModel.setUserNameFieldText(it)
                }
            ) {
                Icon(Icons.Rounded.Person, "")
            }

            UserInfoComposable(
                state.userAddressFieldText,
                "Adresse exacte",
                updateText = {
                    deliveryViewModel.setUserAddressFieldText(it)
                }
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
                    deliveryViewModel.setUserInfo(
                        UserInfo(
                            state.userNameFieldText,
                            state.userAddressFieldText
                        )
                    )
                    datePickerState.selectedDateMillis?.let {
                        deliveryViewModel.saveSelectedDate(
                            date = it
                        )
                    }
                    navigateToCheckout.invoke()
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

        if (state.datePickerVisibility) {
            DatePickerComposable(
                datePickerState = datePickerState,
                shouldRemoveDatePicker = {
                    deliveryViewModel.setIsDatePickerShown(false)
                },
                newDateFieldText = {
                    deliveryViewModel.setDateFieldText(it)
                }
            )
        }

        if (state.showDeliveryPathPicker) {
            DeliveryPathPickerComposable(deliveryViewModel) {
                deliveryViewModel.updateShowDeliveryPathPicker(false)
            }
        }
    }
}