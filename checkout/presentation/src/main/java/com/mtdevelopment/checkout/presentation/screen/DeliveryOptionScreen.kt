package com.mtdevelopment.checkout.presentation.screen

import android.annotation.SuppressLint
import android.location.Geocoder
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
import com.mtdevelopment.checkout.presentation.composable.PermissionManagerComposable
import com.mtdevelopment.checkout.presentation.composable.UserInfoComposable
import com.mtdevelopment.checkout.presentation.composable.getDatePickerState
import com.mtdevelopment.checkout.presentation.model.ShippingDefaultSelectableDates
import com.mtdevelopment.checkout.presentation.model.ShippingSelectableMetaDates
import com.mtdevelopment.checkout.presentation.model.ShippingSelectablePontarlierDates
import com.mtdevelopment.checkout.presentation.model.ShippingSelectableSalinDates
import com.mtdevelopment.checkout.presentation.viewmodel.DeliveryViewModel
import com.mtdevelopment.core.model.DeliveryPath
import com.mtdevelopment.core.presentation.composable.RiveAnimation
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize
import org.koin.androidx.compose.koinViewModel

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

    val state = deliveryViewModel.deliveryUiDataState
    val scrollState = rememberScrollState()
    val columnScrollingEnabled = remember { mutableStateOf(true) }

    if (MapboxOptions.accessToken != MAPBOX_PUBLIC_TOKEN) {
        MapboxOptions.accessToken = MAPBOX_PUBLIC_TOKEN
    }

    val datePickerState =
        when (state.selectedPath) {
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

    LaunchedEffect(state.selectedPath) {
        deliveryViewModel.setIsDatePickerClickable(state.selectedPath != null)
        deliveryViewModel.setDateFieldText("")
    }

    Surface(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(state = scrollState, enabled = columnScrollingEnabled.value)
    ) {
        // Localisation permission
        if (state.shouldShowLocalisationPermission) {
            PermissionManagerComposable(
                onUpdateUserCity = {
                    deliveryViewModel.updateUserCity(it)
                },
                onUpdateSelectedPath = {
                    deliveryViewModel.updateSelectedPath(it)
                },
                onUpdateUserIsOnPath = {
                    deliveryViewModel.updateUserLocationOnPath(it)
                },
                onUpdateUserCityLocation = {
                    deliveryViewModel.updateUserCityLocation(it)
                },
                onUpdateLocalisationState = {
                    deliveryViewModel.updateLocalisationState(it)
                },
                onUpdateShouldShowLocalisationPermission = {
                    deliveryViewModel.updateShouldShowLocalisationPermission(it)
                }
            )
        }

        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {

            // TODO: Fix recomposition HELL
            // Map Card
            MapBoxComposable(
                userLocation = state.userCityLocation,
                chosenPath = state.selectedPath,
                setIsLoading = {
//                    deliveryViewModel.setIsLoading(it)
                },
                setColumnScrollingEnabled = {
//                    columnScrollingEnabled.value = it
                }
            )

            // Localisation Type Picker
            LocalisationTypePicker(
                showDeliverySelection = {
                    deliveryViewModel.updateShowDeliveryPathPicker(true)
                },
                selectedPath = state.selectedPath,
                localisationSuccess = state.localisationSuccess,
                shouldAskLocalisationPermission = {
                    deliveryViewModel.updateShouldShowLocalisationPermission(true)
                }
            )

            if (state.localisationSuccess || state.selectedPath != null || state.userLocationOnPath) {
                LocalisationTextComposable(
                    selectedPath = state.selectedPath,
                    geolocIsOnPath = state.userLocationOnPath,
                    userCity = state.userCity
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
                    deliveryViewModel.saveUserInfo(onError = {
                        // TODO: Manage error
                    })
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

        if (state.isLoading) {
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
            DeliveryPathPickerComposable(selectedPath = state.selectedPath,
                onPathSelected = {
                    deliveryViewModel.updateSelectedPath(it)
                },
                onDismiss = {
                    deliveryViewModel.updateShowDeliveryPathPicker(false)
                })
        }
    }
}