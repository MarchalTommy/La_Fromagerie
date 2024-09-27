package com.mtdevelopment.checkout.presentation.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.styleImportsConfig
import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.checkout.presentation.BuildConfig.MAPBOX_PUBLIC_TOKEN
import com.mtdevelopment.checkout.presentation.R
import com.mtdevelopment.checkout.presentation.model.ShippingSelectableDates
import com.mtdevelopment.checkout.presentation.viewmodel.CheckoutViewModel
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun CheckoutScreen(
    cartViewModel: CartViewModel? = null,
    checkoutViewModel: CheckoutViewModel? = null,
    navigateToHome: () -> Unit = {},
    screenSize: ScreenSize = rememberScreenSize()
) {


    if (MapboxOptions.accessToken != MAPBOX_PUBLIC_TOKEN) {
        MapboxOptions.accessToken = MAPBOX_PUBLIC_TOKEN
    }

    val calendar = Calendar.getInstance()
    val nextJanuary = Calendar.getInstance()
    val thisDecember = Calendar.getInstance()

    LaunchedEffect(true) {
        calendar.time

        nextJanuary.set(calendar.get(Calendar.YEAR) + 1, 1, 14)
        nextJanuary.timeInMillis

        thisDecember.set(calendar.get(Calendar.YEAR), 11, 1)
        calendar.timeInMillis
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = calendar.timeInMillis,
        yearRange = if (calendar.before(nextJanuary) && calendar.after(thisDecember)) {
            IntRange(calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR) + 1)
        } else {
            IntRange(calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR))
        },
        initialDisplayMode = DisplayMode.Picker,
        selectableDates = ShippingSelectableDates()
    )

    val datePickerVisibility = remember { mutableStateOf(false) }
    val dateFieldText = remember { mutableStateOf("") }

    val localisationPermissionState = remember { mutableStateOf(false) }

    val localisationSuccess = remember { mutableStateOf(false) }
    val geolocIsOnPath = remember { mutableStateOf(false) }

    val showDeliveryPathPicker = remember { mutableStateOf(false) }
    fun showDeliverySelection() {
        showDeliveryPathPicker.value = true
    }

    val selectedDeliveryPath = checkoutViewModel?.selectedPath?.collectAsState()

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
            Card(
                modifier = Modifier.heightIn(min = 0.dp, max = (screenSize.height / 5) * 2)
                    .fillMaxWidth().padding(4.dp),
                colors = CardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.primaryContainer),
                    disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    disabledContentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.secondaryContainer)
                ),
                elevation = CardDefaults.elevatedCardElevation()
            ) {
                Card(
                    modifier = Modifier.padding(8.dp).fillMaxSize(),
                    elevation = CardDefaults.elevatedCardElevation()
                ) {
                    MapboxMap(
                        mapViewportState = rememberMapViewportState {
                            setCameraOptions {
                                zoom(8.0)
                                center(Point.fromLngLat(6.356186, 46.773176))
                                pitch(0.0)
                                bearing(0.0)
                            }
                        },
                        style = {
                            MapboxStandardStyle {
                                styleImportsConfig {
                                    mutableMapOf(
                                        Pair(
                                            "Basic",
                                            "mapbox://styles/marchaldevelopment/cm1gh7eih002r01pe96i52fga"
                                        )
                                    )
                                }
                            }
                        }
                    ) {
                        MapEffect(Unit) { mapView ->


                        }
                    }
                }
            }

            Row(
                modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    modifier = Modifier.padding(horizontal = 8.dp).weight(1f),
                    onClick = {
                        localisationPermissionState.value = true
                    },
                    content = {
                        Text(
                            modifier = Modifier.padding(8.dp).align(Alignment.CenterVertically),
                            text = stringResource(R.string.auto_locate),
                            textAlign = TextAlign.Center
                        )
                    })

                VerticalDivider(
                    modifier = Modifier.padding(8.dp).height(64.dp),
                    thickness = Dp.Hairline
                )

                TextButton(
                    modifier = Modifier.padding(horizontal = 8.dp).weight(1f),
                    onClick = {
                        showDeliverySelection()
                    },
                    content = {
                        Text(
                            modifier = Modifier.padding(8.dp).align(Alignment.CenterVertically),
                            text = stringResource(R.string.manually_select_delivery_path),
                            textAlign = TextAlign.Center
                        )
                    })
            }

            if (localisationSuccess.value) {
                Text(
                    modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth(),
                    text = when {
                        // TODO: DEBUG THAT, MANUAL SELECT DOES NOT TRIGGER THIS MESSAGE...
                        selectedDeliveryPath?.value != null -> stringResource(
                            R.string.manual_path_chosen,
                            selectedDeliveryPath.value?.pathName ?: "Unknown"
                        )

                        geolocIsOnPath.value -> stringResource(
                            R.string.auto_geoloc_success,
                            "Métabief"/* todo Replace with user city if supported */
                        )

                        !geolocIsOnPath.value && selectedDeliveryPath?.value == null -> stringResource(R.string.auto_geoloc_not_on_path)

                        else -> "ERROR"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(8.dp),
                thickness = Dp.Hairline
            )

            OutlinedTextField(
                modifier = Modifier.clickable {
                    datePickerVisibility.value = true
                }.padding(horizontal = 8.dp).fillMaxWidth(),
                value = dateFieldText.value,
                onValueChange = { newValue ->
                    dateFieldText.value = newValue
                },
                enabled = false,
                label = {
                    Text(
                        stringResource(R.string.delivery_date_label),
                        Modifier.background(Color.Transparent)
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = ShapeDefaults.Medium,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledSupportingTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedSupportingTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                leadingIcon = {
                    Icon(Icons.Sharp.DateRange, "", Modifier.padding(end = 8.dp))
                }
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