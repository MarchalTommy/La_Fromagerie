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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.TransitionOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.styleImportsConfig
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize

@Composable
fun MapBoxComposable(
    userLocation: State<Pair<Double, Double>?>? = null
) {

    val screenSize: ScreenSize = rememberScreenSize()

    val map = remember { mutableStateOf<MapView?>(null) }

    var pointAnnotationManager: PointAnnotationManager? by remember {
        mutableStateOf(null)
    }

    val point = remember {
        mutableStateOf<Point>(
            Point.fromLngLat(
                userLocation?.value?.second ?: 0.0,
                userLocation?.value?.first ?: 0.0
            )
        )
    }

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
                    MapboxStandardStyle(
                        styleImportsContent = {},
                        styleTransition = TransitionOptions.Builder().duration(2000).build(),
                        middleSlot = {
                            styleImportsConfig {
                                mutableMapOf(
                                    Pair(
                                        "Basic",
                                        "mapbox://styles/marchaldevelopment/cm1s77ihq00m301pl7w12c0kc"
                                    )
                                )
                            }
                        },
                        init = {
                            styleImportsConfig {
                                mutableMapOf(
                                    Pair(
                                        "Basic",
                                        "mapbox://styles/marchaldevelopment/cm1s77ihq00m301pl7w12c0kc"
                                    )
                                )
                            }
                        }
                    )
                },
                scaleBar = { /* Nothing */ }
            ) {
                MapEffect(Unit) { mapView ->
                    map.value = mapView

                    pointAnnotationManager =
                        mapView.annotations.createPointAnnotationManager(AnnotationConfig())

                }
            }
        }
    }

    if (userLocation?.value != null) {
        point.value = Point.fromLngLat(
            userLocation.value?.second ?: 0.0,
            userLocation.value?.first ?: 0.0
        )

        if (point.value.latitude() != 0.0 && point.value.longitude() != 0.0) {
            pointAnnotationManager?.let {
                it.deleteAll()
                val pointAnnotationOptions =
                    PointAnnotationOptions().withPoint(point.value)

                it.create(pointAnnotationOptions)
            }

            map.value?.camera?.flyTo(
                cameraOptions = CameraOptions.Builder()
                    .center(
                        point.value
                    )
                    .zoom(10.0)
                    .build(),
                animationOptions = MapAnimationOptions.mapAnimationOptions {
                    duration(1500)
                }
            )
        }
    }
}