package com.mtdevelopment.checkout.presentation.composable

import android.location.Geocoder
import android.view.MotionEvent
import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.TransitionOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.styleImportsConfig
import com.mapbox.maps.logI
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.addOnMoveListener
import com.mtdevelopment.checkout.presentation.model.DeliveryPath
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MapBoxComposable(
    userLocation: State<Pair<Double, Double>?>? = null,
    chosenPath: State<DeliveryPath?>? = null,
    columnScrollingEnabled: MutableState<Boolean>
) {

    val context = LocalContext.current

    val selectedPathZoomPoint = remember {
        mutableStateOf<Point?>(
            null
        )
    }
    val selectedPathZoomPoint2 = remember {
        mutableStateOf<Point?>(
            null
        )
    }
    val geocoder = Geocoder(context)

    fun getCameraLocalisationFromPath(path: DeliveryPath) {
        val northWestCity = geocoder.getFromLocationName(
            when (path) {
                DeliveryPath.PATH_META -> {
                    "Censeau"
                }

                DeliveryPath.PATH_SALIN -> {
                    "Ivrey"
                }

                DeliveryPath.PATH_PON -> {
                    "Levier"
                }
            }, 1
        )?.first()

        val southEastCity = geocoder.getFromLocationName(
            when (path) {
                DeliveryPath.PATH_META -> {
                    "Jougne"
                }

                DeliveryPath.PATH_SALIN -> {
                    "Boujailles"
                }

                DeliveryPath.PATH_PON -> {
                    "La Cluse-et-Mijoux"
                }
            }, 1
        )?.first()

        northWestCity?.let {
            selectedPathZoomPoint.value = Point.fromLngLat(
                northWestCity.longitude, northWestCity.latitude
            )
        }

        southEastCity?.let {
            selectedPathZoomPoint2.value = Point.fromLngLat(
                southEastCity.longitude, southEastCity.latitude
            )
        }
    }

    fun getBaseCameraLocation(): Point {
        val northWestCity = geocoder.getFromLocationName(
            "Ivrey", 1
        )?.first()

        val southEastCity = geocoder.getFromLocationName(
            "Jougne", 1
        )?.first()

        val northWestPoint = Point.fromLngLat(
            northWestCity?.longitude ?: 0.0, northWestCity?.latitude ?: 0.0
        )
        val southEastPoint = Point.fromLngLat(
            southEastCity?.longitude ?: 0.0, southEastCity?.latitude ?: 0.0
        )

        val long = (southEastPoint.longitude() + northWestPoint.longitude()) / 2
        val lat = (southEastPoint.latitude() + northWestPoint.latitude()) / 2

        return Point.fromLngLat(long, lat)
    }

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

    fun zoomOnSelectedPathMainCity() {
        val long =
            (selectedPathZoomPoint.value?.longitude()
                ?.plus(selectedPathZoomPoint2.value?.longitude() ?: 0.0))?.div(2)

        val lat = selectedPathZoomPoint.value?.latitude()
            ?.plus(selectedPathZoomPoint2.value?.latitude() ?: 0.0)?.div(2)

        map.value?.camera?.flyTo(
            cameraOptions = CameraOptions.Builder()
                .center(
                    Point.fromLngLat(
                        long ?: 0.0,
                        lat ?: 0.0,
                    )
                )
                .zoom(9.5)
                .build(),
            animationOptions = MapAnimationOptions.mapAnimationOptions {
                duration(1500)
            }
        )
    }

    Card(
        modifier = Modifier.heightIn(min = 0.dp, max = (screenSize.height / 5) * 2)
            .fillMaxWidth().padding(4.dp).focusable(true),
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
                modifier = Modifier.pointerInteropFilter(onTouchEvent = {
                    when (it.action) {
                        MotionEvent.ACTION_DOWN -> {
                            columnScrollingEnabled.value = false
                            logI("MapTouch", "PRESSED DOWN")
                            false
                        }

                        else -> {
                            false
                        }
                    }
                }),
                mapViewportState = rememberMapViewportState {
                    setCameraOptions {
                        zoom(8.5)
                        center(
                            getBaseCameraLocation()
                        )
                        pitch(0.0)
                        bearing(0.0)
                    }
                },
                style = {
                    MapboxStandardStyle(
                        styleImportsContent = {},
                        styleTransition = TransitionOptions.Builder().duration(2000).build(),
                        middleSlot = {

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

                    mapView.mapboxMap.addOnMoveListener(object : OnMoveListener {
                        override fun onMove(detector: MoveGestureDetector): Boolean {
                            //Do nothing
                            return false
                        }

                        override fun onMoveBegin(detector: MoveGestureDetector) {
                            //Do nothing
                        }

                        override fun onMoveEnd(detector: MoveGestureDetector) {
                            columnScrollingEnabled.value = true
                            logI("MapTouch", "RELEASED")
                        }

                    })

                    pointAnnotationManager =
                        mapView.annotations.createPointAnnotationManager(AnnotationConfig())

                    mapView.mapboxMap.loadStyle(
                        "mapbox://styles/marchaldevelopment/cm1s77ihq00m301pl7w12c0kc"
                    ) {
                        // TODO: Lottie Loader
                    }
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

    when (chosenPath?.value) {
        DeliveryPath.PATH_META -> {
            getCameraLocalisationFromPath(DeliveryPath.PATH_META)
            map.value?.mapboxMap?.loadStyle(
                "mapbox://styles/marchaldevelopment/cm1te6xn5018j01qphimw2wuz"
            ) {
                zoomOnSelectedPathMainCity()
            }
        }

        DeliveryPath.PATH_SALIN -> {
            getCameraLocalisationFromPath(DeliveryPath.PATH_SALIN)
            map.value?.mapboxMap?.loadStyle(
                "mapbox://styles/marchaldevelopment/cm1te6tb700om01pl70i6avlk"
            ) {
                zoomOnSelectedPathMainCity()
            }
        }

        DeliveryPath.PATH_PON -> {
            getCameraLocalisationFromPath(DeliveryPath.PATH_PON)
            map.value?.mapboxMap?.loadStyle(
                "mapbox://styles/marchaldevelopment/cm1teahes00xi01qrghgr91ku"
            ) {
                zoomOnSelectedPathMainCity()
            }
        }

        else -> {
            map.value?.mapboxMap?.loadStyle(
                "mapbox://styles/marchaldevelopment/cm1s77ihq00m301pl7w12c0kc"
            )
        }
    }
}