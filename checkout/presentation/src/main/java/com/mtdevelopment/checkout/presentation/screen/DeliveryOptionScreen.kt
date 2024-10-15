package com.mtdevelopment.checkout.presentation.screen

import android.annotation.SuppressLint
import android.location.Address
import android.location.Geocoder
import android.os.Build
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import app.rive.runtime.kotlin.core.Rive
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mapbox.android.core.permissions.PermissionsManager.Companion.areLocationPermissionsGranted
import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
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
import com.mtdevelopment.checkout.presentation.viewmodel.DeliveryUiState
import com.mtdevelopment.checkout.presentation.viewmodel.DeliveryViewModel
import com.mtdevelopment.checkout.presentation.viewmodel.LOCALISATION_ERROR
import com.mtdevelopment.core.presentation.composable.RiveAnimation
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize
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
    // Never seems to work on emulator ?! What about real world then...
    // TODO: FIX LOADER ->
    // Is still blocked by UI thread breaking when Geocoder bug. Place geocoder in custom thread ?
    // Removes itself just after the delay, not after boolean set to false OR delay. Bad brain of me.


    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }

    val context = LocalContext.current

    val cartViewModel = koinViewModel<CartViewModel>()
    val deliveryViewModel =
        koinViewModel<DeliveryViewModel>(viewModelStoreOwner = viewModelStoreOwner)

    val screenState by deliveryViewModel.deliveryUiState.collectAsStateWithLifecycle()
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    val geocoder = Geocoder(context)

    val scrollState = rememberScrollState()
    val columnScrollingEnabled = remember { mutableStateOf(true) }
    val selectedPath =
        remember { mutableStateOf((screenState as? DeliveryUiState.DeliveryDataState)?.path) }

    val shouldDatePickerBeClickable = remember { mutableStateOf(false) }
    val dateFieldText = remember { mutableStateOf("") }
    val userNameFieldText = remember {
        mutableStateOf(
            (screenState as? DeliveryUiState.DeliveryDataState)?.userInfo?.userName ?: ""
        )
    }
    val userAddressFieldText = remember {
        mutableStateOf(
            (screenState as? DeliveryUiState.DeliveryDataState)?.userInfo?.userAddress ?: ""
        )
    }

    val localisationPermissionState = remember { mutableStateOf(false) }

    val localisationSuccess =
        remember { mutableStateOf(screenState is DeliveryUiState.LocationSuccess) }
    val geolocIsOnPath =
        remember { mutableStateOf(screenState is DeliveryUiState.LocalisationNotOnPath) }

    val userCity = remember { mutableStateOf("") }

    val datePickerState =
        when (selectedPath.value) {
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

    fun getCityFromGeocoder(addressesList: List<Address>?) {
        val foundAddress = addressesList?.find { address ->
            val correctPath =
                DeliveryPath.entries.find { path ->
                    path.availableCities.contains(
                        address.locality
                    )
                }
            if (correctPath != null) {
                deliveryViewModel.manageScreenState(path = correctPath)
                true
            } else {
                deliveryViewModel.manageScreenState(localisationError = LOCALISATION_ERROR.NOT_ON_PATH)
                false
            }
        }
        foundAddress?.locality?.let { locality ->
            userCity.value = locality
        }
    }

    LaunchedEffect(Unit) {
        Rive.init(context)
    }

    LaunchedEffect(selectedPath) {
        shouldDatePickerBeClickable.value = selectedPath.value != null
        dateFieldText.value = ""
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        // Localisation permission
        if (localisationPermissionState.value) {
            RequestLocationPermission(
                onPermissionGranted = {
                    fusedLocationProviderClient =
                        LocationServices.getFusedLocationProviderClient(context)
                    getLastLocation(onGetLastLocationSuccess = {
                        deliveryViewModel.manageScreenState(localisationSuccess = true)
                        deliveryViewModel.manageScreenState(userLocation = it)
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
                            deliveryViewModel.manageScreenState(localisationError = LOCALISATION_ERROR.CANNOT_GET_LOCATION)
                            userCity.value = "Unknown"
                        })
                },
                onPermissionDenied = {
                    deliveryViewModel.manageScreenState(localisationError = LOCALISATION_ERROR.PERMISSION_REFUSED)
                    localisationPermissionState.value = false
                },
                onPermissionsRevoked = {
                    deliveryViewModel.manageScreenState(localisationError = LOCALISATION_ERROR.PERMISSION_REFUSED)
                    localisationPermissionState.value = false
                }
            )
        }

        // TODO: Actually IDK why but recomposing all the time without even touching it.
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp)
                .verticalScroll(state = scrollState, enabled = columnScrollingEnabled.value)
        ) {

            // Map Card
            MapBoxComposable(
                viewModelStoreOwner = viewModelStoreOwner,
                columnScrollState = {
                    columnScrollingEnabled.value = it
                },
            )

            // Localisation Type Picker
            LocalisationTypePicker(
                hasSelectedPath = selectedPath.value != null,
                isLocalisationSuccessful = screenState is DeliveryUiState.LocationSuccess,
                showDeliverySelection = {
                    deliveryViewModel.manageScreenState(shouldShowPathSelection = true)
                },
                setLocalisationPermission = {
                    localisationPermissionState.value = it
                }
            )

            if (screenState is DeliveryUiState.LocationSuccess ||
                selectedPath.value != null ||
                screenState is DeliveryUiState.LocalisationNotOnPath
            ) {
                LocalisationTextComposable(
                    selectedPath = selectedPath.value,
                    geolocIsOnPath = screenState is DeliveryUiState.LocalisationNotOnPath,
                    userCity = userCity.value
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(8.dp),
                thickness = Dp.Hairline
            )

            DateTextField(
                shouldBeClickable = shouldDatePickerBeClickable.value,
                dateFieldText = dateFieldText.value,
                datePickerState = datePickerState,
                showDatePicker = {
                    deliveryViewModel.manageScreenState(shouldShowDateSelection = true)
                },
                setDateTextFieldText = {
                    dateFieldText.value = it
                }
            )

            UserInfoComposable(
                userNameFieldText.value,
                "Nom complet",
                setText = {
                    deliveryViewModel.manageScreenState(userName = it)
                }
            ) {
                Icon(Icons.Rounded.Person, "")
            }

            UserInfoComposable(
                userAddressFieldText.value,
                "Adresse exacte",
                setText = {
                    deliveryViewModel.manageScreenState(userAddress = it)
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
//                    deliveryViewModel.test()
//                    checkoutViewModel.setUserInfo(
//                        UserInfo(
//                            userNameFieldText.value,
//                            userAddressFieldText.value
//                        )
//                    )
//                    navigateToCheckout.invoke()
                }) {
                Text("Valider et passer au paiement")
            }
        }

//        LaunchedEffect(isLoading.value) {
//            val currentValue = isLoading.value
//            val showUpDelay = 200L
//            val removeDelay = 1000L
//            CoroutineScope(Dispatchers.Default).launch {
//                delay(if (isLoading.value) showUpDelay else removeDelay)
//                // Should show loading only if state hasn't change in last 200ms
//                if (isLoading.value == currentValue && isLoading.value) {
//                    Log.i(TAG, "DeliveryOptionScreen: SHOWING LOADER")
//                    shouldShowLoading.value = true
//                } else if (isLoading.value == currentValue && !isLoading.value) {
//                    Log.i(TAG, "DeliveryOptionScreen: REMOVING LOADER")
//                    shouldShowLoading.value = false
//                }
//            }
//        }

        if (screenState is DeliveryUiState.Loading) {
            RiveAnimation(
                modifier = Modifier.fillMaxSize(),
                resId = R.raw.goat_loading,
                contentDescription = "Loading animation"
            )
        }

        if (screenState is DeliveryUiState.DateSelection) {
            DatePickerComposable(
                datePickerState = datePickerState,
                setDateTextFieldText = {
                    dateFieldText.value = it
                },
                setDatePickerVisibility = {
                    deliveryViewModel.manageScreenState(shouldShowDateSelection = false)
                }
            )
        }

        if (screenState is DeliveryUiState.PathSelection) {
            DeliveryPathPickerComposable(viewModelStoreOwner) {
                deliveryViewModel.manageScreenState(shouldShowPathSelection = false)
            }
        }
    }
}