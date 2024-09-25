package com.mtdevelopment.checkout.presentation.composable

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.styleImportsConfig
import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.checkout.presentation.BuildConfig.MAPBOX_PUBLIC_TOKEN
import com.mtdevelopment.checkout.presentation.model.ShippingSelectableDates
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun CheckoutScreen(
    cartViewModel: CartViewModel? = null,
    navigateToHome: () -> Unit = {},
    screenSize: ScreenSize = rememberScreenSize()
) {


    if (MapboxOptions.accessToken != MAPBOX_PUBLIC_TOKEN) {
        // TODO: uncomment to work on the map. Keep commented otherwise to avoid unnecessary token use
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

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {

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
                // TODO: uncomment to work on the map. Keep commented otherwise to avoid unnecessary token use
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

        // TODO: "D'après votre géolocalisation, vous êtes sur le circuit de livraison du... etc. 

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
                Text("Date de livraison :", Modifier.background(Color.Transparent))
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
            DatePickerDialog(
                modifier = Modifier,
                onDismissRequest = {
                    dateFieldText.value = datePickerState.selectedDateMillis?.let {
                        SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(it)
                    } ?: ""
                    datePickerVisibility.value = false
                },
                confirmButton = {
                    Text("OK", Modifier.padding(16.dp).clickable {
                        dateFieldText.value = datePickerState.selectedDateMillis?.let {
                            SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(it)
                        } ?: ""
                        datePickerVisibility.value = false
                    })
                },
                dismissButton = {
                    Text("Annuler", Modifier.padding(16.dp).clickable {
                        datePickerVisibility.value = false
                    })
                },
                shape = ShapeDefaults.Medium,
                tonalElevation = 24.dp,
                colors = DatePickerDefaults.colors(),
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    usePlatformDefaultWidth = true
                ),
                content = {
                    DatePicker(
                        modifier = Modifier,
                        state = datePickerState,
                        showModeToggle = false,
                        title = {
                            Text(
                                modifier = Modifier.padding(top = 16.dp, start = 16.dp),
                                text = "Quand souhaitez-vous être livré ?"
                            )
                        }
                    )
                }
            )
        }
    }


}