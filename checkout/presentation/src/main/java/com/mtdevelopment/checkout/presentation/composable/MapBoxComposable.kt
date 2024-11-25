package com.mtdevelopment.checkout.presentation.composable

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.location.Address
import android.location.Geocoder
import android.os.Build
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
import com.mtdevelopment.core.model.DeliveryPath
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val FRASNE_LATITUDE = 46.854022
const val FRASNE_LONGITUDE = 6.156132

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MapBoxComposable(
    userLocation: Pair<Double, Double>?,
    chosenPath: DeliveryPath?,
    isConnectedToInternet: Boolean,
    setIsLoading: (Boolean) -> Unit,
    setColumnScrollingEnabled: (Boolean) -> Unit,
    onError: (String) -> Unit = {}
) {

    val context = LocalContext.current
    val cameraBasePoint =
        remember { mutableStateOf(Point.fromLngLat(FRASNE_LONGITUDE, FRASNE_LATITUDE)) }

    val geocoder = Geocoder(context)
    val screenSize: ScreenSize = rememberScreenSize()
    val map = remember { mutableStateOf<MapView?>(null) }

    var pointAnnotationManager: PointAnnotationManager? by remember {
        mutableStateOf(null)
    }
    val hasBeenGeoloked = remember { mutableStateOf(false) }

    val userPoint = remember {
        mutableStateOf<Point>(
            Point.fromLngLat(
                userLocation?.second ?: 0.0,
                userLocation?.first ?: 0.0
            )
        )
    }

    val animatorListener = object : AnimatorListener {
        override fun onAnimationStart(p0: Animator) {
            // Is here only to assure loader goes away after animating has started,
            // which means style has loaded
            setIsLoading.invoke(false)
            hasBeenGeoloked.value = true
        }

        override fun onAnimationEnd(p0: Animator) {
            /* NOTHING */
        }

        override fun onAnimationCancel(p0: Animator) {
            /* NOTHING */
        }

        override fun onAnimationRepeat(p0: Animator) {
            /* NOTHING */
        }
    }

    LaunchedEffect(Unit) {
        getBaseCameraLocation(geocoder, setIsLoading = { setIsLoading.invoke(true) },
            callback = {
                cameraBasePoint.value = it
            })
    }

    LaunchedEffect(chosenPath) {
        if (chosenPath != null) {
            if (isConnectedToInternet) {
                getCameraLocalisationFromPath(
                    chosenPath,
                    geocoder,
                    setIsLoading,
                    onBothPointFound = { nw, se ->
                        if (map.value?.mapboxMap?.style?.styleURI != chosenPath.mapStyle) {
                            map.value?.mapboxMap?.loadStyle(
                                chosenPath.mapStyle
                            ) {
                                zoomOnSelectedPathMainCity(
                                    map.value,
                                    nw,
                                    se,
                                    animatorListener
                                )
                            }
                        }
                    }
                )
            } else {
                zoomOnSelectedPathOffline(
                    map.value,
                    animatorListener
                )
            }
        } else {
            if (map.value?.mapboxMap?.style == null) {
                map.value?.mapboxMap?.loadStyle(
                    "mapbox://styles/marchaldevelopment/cm1s77ihq00m301pl7w12c0kc"
                ) {
                    setIsLoading.invoke(false)
                }
            }
        }
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
                            setColumnScrollingEnabled.invoke(false)
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
                            cameraBasePoint.value
                        )
                        pitch(0.0)
                        bearing(0.0)
                    }
                },
                style = {
                    MapboxStandardStyle(
                        styleImportsContent = {},
                        styleTransition = TransitionOptions.Builder().duration(2000).build(),
                        middleSlot = {},
                        init = {
                            styleImportsConfig {
                                mutableMapOf(
                                    Pair(
                                        "Basic",
                                        chosenPath?.mapStyle
                                            ?: "mapbox://styles/marchaldevelopment/cm1s77ihq00m301pl7w12c0kc"
                                    )
                                )
                            }
                        }
                    )
                },
                scaleBar = { /* Nothing */ },
                attribution = { /* Nothing */ },
                logo = { /* Nothing */ }
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
                            setColumnScrollingEnabled.invoke(true)
                        }

                    })

                    pointAnnotationManager =
                        mapView.annotations.createPointAnnotationManager(AnnotationConfig())

                    setIsLoading.invoke(true)
                    if (mapView.mapboxMap.style == null) {
                        mapView.mapboxMap.loadStyle(
                            chosenPath?.mapStyle
                                ?: "mapbox://styles/marchaldevelopment/cm1s77ihq00m301pl7w12c0kc"
                        ) {
                            setIsLoading.invoke(false)
                        }
                    } else {
                        setIsLoading.invoke(false)
                    }
                }
            }
        }
    }

    if (userLocation != null && chosenPath == null) {
        userPoint.value = Point.fromLngLat(
            userLocation.second,
            userLocation.first
        )

        if (userPoint.value.latitude() != 0.0 &&
            userPoint.value.longitude() != 0.0 &&
            !hasBeenGeoloked.value
        ) {
            pointAnnotationManager?.let {
                it.deleteAll()
                val pointAnnotationOptions =
                    PointAnnotationOptions().withPoint(userPoint.value)

                it.create(pointAnnotationOptions)
            }

            map.value?.camera?.flyTo(
                cameraOptions = CameraOptions.Builder()
                    .center(
                        userPoint.value
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

fun selectNorthWestCityFromPath(path: DeliveryPath): String {
    return when (path) {
        DeliveryPath.PATH_META -> {
            "Censeau"
        }

        DeliveryPath.PATH_SALIN -> {
            "Ivrey"
        }

        DeliveryPath.PATH_PON -> {
            "Levier"
        }
    }
}

fun selectSouthEastCityFromPath(path: DeliveryPath): String {
    return when (path) {
        DeliveryPath.PATH_META -> {
            "Jougne"
        }

        DeliveryPath.PATH_SALIN -> {
            "Boujailles"
        }

        DeliveryPath.PATH_PON -> {
            "La Cluse-et-Mijoux"
        }
    }
}

fun getCameraLocalisationFromPath(
    path: DeliveryPath,
    geocoder: Geocoder,
    setIsLoading: (Boolean) -> Unit,
    onBothPointFound: (pointNW: Point, pointSE: Point) -> Unit
) {
    setIsLoading.invoke(true)
    val scope = CoroutineScope(Dispatchers.Main)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        scope.launch {
            val pointNW = async {
                suspendCoroutine {
                    geocoder.getFromLocationName(
                        selectNorthWestCityFromPath(path),
                        1
                    ) { addressList ->
                        it.resume(
                            Point.fromLngLat(
                                addressList.firstOrNull()?.longitude ?: 0.0,
                                addressList.firstOrNull()?.latitude ?: 0.0
                            )
                        )
                    }
                }
            }.await()

            val pointSE = async {
                suspendCoroutine {
                    geocoder.getFromLocationName(
                        selectSouthEastCityFromPath(path),
                        1
                    ) { addressList ->
                        it.resume(
                            Point.fromLngLat(
                                addressList.firstOrNull()?.longitude ?: 0.0,
                                addressList.firstOrNull()?.latitude ?: 0.0
                            )
                        )
                    }
                }
            }.await()

            setIsLoading.invoke(false)
            onBothPointFound.invoke(pointNW, pointSE)
        }
    } else {
        try {
            val addressNW = geocoder.getFromLocationName(
                selectNorthWestCityFromPath(path), 1
            )?.firstOrNull()
            val pointNW = Point.fromLngLat(
                addressNW?.longitude ?: 0.0,
                addressNW?.latitude ?: 0.0
            )

            val addressSE = geocoder.getFromLocationName(
                selectSouthEastCityFromPath(path), 1
            )?.firstOrNull()
            val pointSE = Point.fromLngLat(
                addressSE?.longitude ?: 0.0,
                addressSE?.latitude ?: 0.0
            )

            onBothPointFound.invoke(pointNW, pointSE)
        } catch (e: IOException) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }
}

fun getBaseCameraLocation(
    geocoder: Geocoder,
    setIsLoading: (Boolean) -> Unit,
    callback: (Point) -> Unit
) {
    setIsLoading.invoke(true)
    CoroutineScope(Dispatchers.IO).launch {
        var northWestCity: Address? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocationName(
                "Ivrey",
                1
            ) { addressList ->
                northWestCity = addressList.firstOrNull()
            }
        } else {
            northWestCity = try {
                geocoder.getFromLocationName(
                    "Ivrey", 1
                )?.firstOrNull()
            } catch (e: IOException) {
                null
            }
        }

        var southEastCity: Address? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocationName(
                "Jougne",
                1
            ) { addressList ->
                southEastCity = addressList.firstOrNull()
            }
        } else {
            southEastCity = try {
                geocoder.getFromLocationName(
                    "Jougne", 1
                )?.firstOrNull()
            } catch (e: IOException) {
                null
            }
        }

        if (northWestCity == null || southEastCity == null) {
            callback.invoke(Point.fromLngLat(FRASNE_LONGITUDE, FRASNE_LATITUDE))
            return@launch
        }

        val northWestPoint = Point.fromLngLat(
            (northWestCity as Address).longitude, northWestCity!!.latitude
        )
        val southEastPoint = Point.fromLngLat(
            (southEastCity as Address).longitude, southEastCity!!.latitude
        )

        val long = (southEastPoint.longitude() + northWestPoint.longitude()) / 2
        val lat = (southEastPoint.latitude() + northWestPoint.latitude()) / 2

        callback.invoke(Point.fromLngLat(long, lat))
    }
}

fun zoomOnSelectedPathMainCity(
    map: MapView?,
    selectedPathNW: Point?,
    selectedPathSE: Point?,
    animatorListener: AnimatorListener
) {
    val long =
        (selectedPathNW?.longitude()
            ?.plus(selectedPathSE?.longitude() ?: 0.0))?.div(2)

    val lat = selectedPathNW?.latitude()
        ?.plus(selectedPathSE?.latitude() ?: 0.0)?.div(2)

    map?.camera?.flyTo(
        cameraOptions = CameraOptions.Builder()
            .center(
                Point.fromLngLat(
                    long ?: FRASNE_LONGITUDE,
                    lat ?: FRASNE_LATITUDE,
                )
            )
            .zoom(if (lat != null && long != null) 9.5 else 8.5)
            .build(),
        animationOptions = MapAnimationOptions.mapAnimationOptions {
            duration(1500)
        },
        animatorListener = animatorListener
    )
}

// TODO: Draw paths in local to allow for an offline working map.
// TODO: Save paths on firebase, fetch them when changed, and allow admin to update them (still needs to find how)
fun zoomOnSelectedPathOffline(
    map: MapView?,
    animatorListener: AnimatorListener
) {
    map?.camera?.flyTo(
        cameraOptions = CameraOptions.Builder()
            .center(
                Point.fromLngLat(
                    FRASNE_LONGITUDE,
                    FRASNE_LATITUDE,
                )
            )
            .zoom(8.5)
            .build(),
        animationOptions = MapAnimationOptions.mapAnimationOptions {
            duration(1500)
        },
        animatorListener = animatorListener
    )
}