package com.mtdevelopment.checkout.presentation.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mapbox.common.MapboxOptions
import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.checkout.presentation.BuildConfig.MAPBOX_PUBLIC_TOKEN
import com.mtdevelopment.checkout.presentation.model.DeliveryPath
import com.mtdevelopment.checkout.presentation.model.ShippingDefaultSelectableDates
import com.mtdevelopment.checkout.presentation.model.ShippingSelectableMetaDates
import com.mtdevelopment.checkout.presentation.model.ShippingSelectablePontarlierDates
import com.mtdevelopment.checkout.presentation.model.ShippingSelectableSalinDates
import com.mtdevelopment.checkout.presentation.viewmodel.CheckoutViewModel
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun DeliveryOptionScreen(
    cartViewModel: CartViewModel? = null,
    checkoutViewModel: CheckoutViewModel? = null,
    navigateToHome: () -> Unit = {},
    screenSize: ScreenSize = rememberScreenSize()
) {

    // TODO: CLEAN THIS HELL OF A FILE :
    // EXTRACT, PUT INTO OWN COMPONENT, AND EXTRACT TO VM OR EVEN DOMAIN MORE LOGIC.
    if (MapboxOptions.accessToken != MAPBOX_PUBLIC_TOKEN) {
        MapboxOptions.accessToken = MAPBOX_PUBLIC_TOKEN
    }

    val calendar = Calendar.getInstance()
    val nextJanuary = Calendar.getInstance()
    val thisDecember = Calendar.getInstance()
    val tomorrow = Calendar.getInstance()
    val nextSelectableDate = Calendar.getInstance()

    LaunchedEffect(true) {
        calendar.time

        nextJanuary.set(calendar.get(Calendar.YEAR) + 1, 1, 14)
        nextJanuary.timeInMillis

        thisDecember.set(calendar.get(Calendar.YEAR), 11, 1)
        calendar.timeInMillis
    }

    val selectedPath = checkoutViewModel?.selectedPath?.collectAsState()
    val nextSelectableDatesList = remember { mutableStateListOf<Long>() }

    val datePickerState =
        when (selectedPath?.value) {

            DeliveryPath.PATH_META -> {
                nextSelectableDatesList.clear()
                for (days in calendar.get(Calendar.DAY_OF_YEAR) + 2..365) {
                    nextSelectableDate.set(Calendar.DAY_OF_YEAR, days)

                    if (ShippingSelectableMetaDates().isSelectableDate(nextSelectableDate.timeInMillis)) {
                        nextSelectableDatesList.add(nextSelectableDate.timeInMillis)
                    }
                }

                rememberDatePickerState(
                    initialSelectedDateMillis = nextSelectableDatesList.first(),
                    yearRange = if (calendar.before(nextJanuary) && calendar.after(thisDecember)) {
                        IntRange(calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR) + 1)
                    } else {
                        IntRange(calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR))
                    },
                    initialDisplayMode = DisplayMode.Picker,
                    selectableDates = ShippingSelectableMetaDates()
                )
            }

            DeliveryPath.PATH_SALIN -> {
                nextSelectableDatesList.clear()
                for (days in calendar.get(Calendar.DAY_OF_YEAR) + 2..365) {
                    nextSelectableDate.set(Calendar.DAY_OF_YEAR, days)

                    if (ShippingSelectableSalinDates().isSelectableDate(nextSelectableDate.timeInMillis)) {
                        nextSelectableDatesList.add(nextSelectableDate.timeInMillis)
                    }
                }

                rememberDatePickerState(
                    initialSelectedDateMillis = nextSelectableDatesList.first(),
                    yearRange = if (calendar.before(nextJanuary) && calendar.after(thisDecember)) {
                        IntRange(calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR) + 1)
                    } else {
                        IntRange(calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR))
                    },
                    initialDisplayMode = DisplayMode.Picker,
                    selectableDates = ShippingSelectableSalinDates()
                )
            }

            DeliveryPath.PATH_PON -> {
                nextSelectableDatesList.clear()
                for (days in calendar.get(Calendar.DAY_OF_YEAR) + 2..365) {
                    nextSelectableDate.set(Calendar.DAY_OF_YEAR, days)

                    if (ShippingSelectablePontarlierDates().isSelectableDate(nextSelectableDate.timeInMillis)) {
                        nextSelectableDatesList.add(nextSelectableDate.timeInMillis)
                    }
                }

                rememberDatePickerState(
                    initialSelectedDateMillis = nextSelectableDatesList.first(),
                    yearRange = if (calendar.before(nextJanuary) && calendar.after(thisDecember)) {
                        IntRange(calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR) + 1)
                    } else {
                        IntRange(calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR))
                    },
                    initialDisplayMode = DisplayMode.Picker,
                    selectableDates = ShippingSelectablePontarlierDates()
                )
            }

            else -> {
                tomorrow.set(Calendar.DAY_OF_YEAR, (calendar.get(Calendar.DAY_OF_YEAR) + 1))
                rememberDatePickerState(
                    initialSelectedDateMillis = tomorrow.timeInMillis,
                    yearRange = if (calendar.before(nextJanuary) && calendar.after(thisDecember)) {
                        IntRange(calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR) + 1)
                    } else {
                        IntRange(calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR))
                    },
                    initialDisplayMode = DisplayMode.Picker,
                    selectableDates = ShippingDefaultSelectableDates()
                )
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

    Surface(modifier = Modifier.fillMaxSize()) {

        // Localisation permission
        if (localisationPermissionState.value) {
            RequestLocationPermission(
                onPermissionGranted = {
                    localisationSuccess.value = true
                    geolocIsOnPath.value =
                        true /* DO NOT FORGET TO REMOVE, FOR DEBUG PURPOSE ONLY */
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
            MapBoxComposable()

            // Localisation Type Picker
            LocalisationTypePicker(
                localisationPermissionState = localisationPermissionState,
                showDeliverySelection = { showDeliverySelection() },
                selectedPath = selectedPath
            )

            if (localisationSuccess.value || selectedPath?.value != null || geolocIsOnPath.value) {
                LocalisationTextComposable(
                    selectedPath = selectedPath,
                    geolocIsOnPath = geolocIsOnPath
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