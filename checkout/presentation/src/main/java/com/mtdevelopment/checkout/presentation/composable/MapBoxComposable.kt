package com.mtdevelopment.checkout.presentation.composable

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.styleImportsConfig
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize

@Composable
fun MapBoxComposable() {

    val screenSize: ScreenSize = rememberScreenSize()

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
}