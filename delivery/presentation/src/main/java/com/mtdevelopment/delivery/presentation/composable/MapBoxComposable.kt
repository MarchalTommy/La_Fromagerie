package com.mtdevelopment.delivery.presentation.composable

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.content.ContentValues.TAG
import android.util.Log
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.TransitionOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.styleImportsConfig
import com.mapbox.maps.extension.style.StyleContract
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style
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
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize
import com.mtdevelopment.delivery.presentation.model.UiDeliveryPath
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

const val FRASNE_LATITUDE = 46.854022
const val FRASNE_LONGITUDE = 6.156132

val pathsColors = mutableListOf(
    "#FF6B6B", "#6BCEFF", "#ff9b54", "#4ECDC4", "#6B6BFF"
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MapBoxComposable(
    userLocation: Pair<Double, Double>?,
    chosenPath: UiDeliveryPath?,
    allPaths: List<UiDeliveryPath>,
    isConnectedToInternet: Boolean,
    setIsLoading: (Boolean) -> Unit,
    setColumnScrollingEnabled: (Boolean) -> Unit,
    onError: (String) -> Unit = {}
) {

    val cameraBasePoint =
        remember { mutableStateOf<Point?>(null) }

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

    LaunchedEffect(allPaths) {
        getBaseCameraLocation(
            allPaths = allPaths, setIsLoading = { setIsLoading.invoke(it) },
            callback = { nw, se ->
                setIsLoading.invoke(false)

                val long =
                    (nw.longitude()
                        .plus(se.longitude())).div(2)

                val lat = nw.latitude()
                    .plus(se.latitude()).div(2)

                cameraBasePoint.value = Point.fromLngLat(
                    long,
                    lat,
                )

                zoomOnSelectedPathMainCity(
                    map.value,
                    nw,
                    se,
                    9.0,
                    null
                )
            })
    }

    LaunchedEffect(chosenPath) {
        if (chosenPath != null) {
            if (isConnectedToInternet) {
                getCameraLocalisationFromPath(
                    chosenPath,
                    onBothPointFound = { nw, se ->
                        map.value?.mapboxMap?.loadStyle(
                            getStyleForPath(chosenPath, allPaths)
                        ) {
                            zoomOnSelectedPathMainCity(
                                map.value,
                                nw,
                                se,
                                null,
                                animatorListener
                            )
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
            if (map.value?.mapboxMap?.style == null && allPaths.isNotEmpty()) {
                map.value?.mapboxMap?.loadStyle(
                    getStyleForNoChosenPath(allPaths)
                ) {
                    setIsLoading.invoke(false)
                }
            }
            map.value?.camera?.flyTo(
                cameraOptions = CameraOptions.Builder()
                    .center(
                        cameraBasePoint.value
                    )
                    .zoom(8.5)
                    .build(),
                animationOptions = MapAnimationOptions.mapAnimationOptions {
                    duration(1500)
                }
            )
        }
    }

    Card(
        modifier = Modifier
            .heightIn(min = 0.dp, max = (screenSize.height / 5) * 2)
            .fillMaxWidth()
            .padding(4.dp)
            .focusable(true),
        colors = CardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.primaryContainer),
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.secondaryContainer)
        ),
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize(),
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
                                getStyleForInit(map, allPaths)
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
                        if (allPaths.isNotEmpty()) {
                            mapView.mapboxMap.loadStyle(
                                if (chosenPath != null) {
                                    getStyleForPath(chosenPath, allPaths)
                                } else {
                                    getStyleForNoChosenPath(allPaths)
                                }
                            ) {
                                setIsLoading.invoke(false)
                            }
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

fun getCameraLocalisationFromPath(
    path: UiDeliveryPath,
    onBothPointFound: (pointNW: Point, pointSE: Point) -> Unit
) {

    val latitudes = path.locations?.map { it.first }
    val maxLatitude = latitudes?.maxOf { it }
    val minLatitude = latitudes?.minOf { it }

    val longitude = path.locations?.map { it.second }
    val maxLongitude = longitude?.maxOf { it }
    val minLongitude = longitude?.minOf { it }

    val northWestPoint = Point.fromLngLat(
        minLongitude ?: 0.0, maxLatitude ?: 0.0
    )
    val southEastPoint = Point.fromLngLat(
        maxLongitude ?: 0.0, minLatitude ?: 0.0
    )

    onBothPointFound.invoke(northWestPoint, southEastPoint)

}

fun getBaseCameraLocation(
    allPaths: List<UiDeliveryPath>,
    setIsLoading: (Boolean) -> Unit,
    callback: (nw: Point, se: Point) -> Unit
) {
    setIsLoading.invoke(true)
    if (allPaths.isNotEmpty()) {
        val allCoordinates = allPaths.flatMap { it.locations!! }

        val latitudes = allCoordinates.map { it.first }
        val maxLatitude = latitudes.maxOf { it }
        val minLatitude = latitudes.minOf { it }

        val longitude = allCoordinates.map { it.second }
        val maxLongitude = longitude.maxOf { it }
        val minLongitude = longitude.minOf { it }

        val northWestPoint = Point.fromLngLat(
            minLongitude, maxLatitude
        )
        val southEastPoint = Point.fromLngLat(
            maxLongitude, minLatitude
        )

        callback.invoke(northWestPoint, southEastPoint)
    }
}

fun zoomOnSelectedPathMainCity(
    map: MapView?,
    selectedPathNW: Point?,
    selectedPathSE: Point?,
    zoom: Double? = null,
    animatorListener: AnimatorListener?
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
            .zoom(
                if (zoom != null) {
                    zoom
                } else {
                    if (lat != null && long != null) {
                        9.5
                    } else {
                        8.5
                    }
                },
            )
            .build(),
        animationOptions = MapAnimationOptions.mapAnimationOptions {
            duration(1500)
        },
        animatorListener = animatorListener
    )
}

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

fun getStyleForPath(
    path: UiDeliveryPath,
    allPaths: List<UiDeliveryPath>
): StyleContract.StyleExtension {
    return style(style = Style.STANDARD) {
        try {
            +geoJsonSource("startingSource") {
                featureCollection(
                    FeatureCollection.fromJson(Json.encodeToString(path.geoJson)),
                    "${path.hashCode()}_data"
                )
            }
            +lineLayer("linelayer", "startingSource") {
                lineCap(LineCap.ROUND)
                lineJoin(LineJoin.ROUND)
                lineOpacity(1.0)
                lineWidth(6.0)
                lineColor(pathsColors[allPaths.indexOf(path) % pathsColors.size])
            }
        } catch (e: NullPointerException) {
            Log.e(TAG, "Style not found for map... Geojson error ?")
        }
    }
}

fun getStyleForNoChosenPath(allPaths: List<UiDeliveryPath>): StyleContract.StyleExtension {

    return style(style = Style.STANDARD) {
        allPaths.forEach {
            +geoJsonSource("${it.hashCode()}") {
                featureCollection(
                    FeatureCollection.fromJson(Json.encodeToString(it.geoJson)),
                    "${it.hashCode()}_data"
                )
            }
            +lineLayer("linelayer", "${it.hashCode()}") {
                lineCap(LineCap.ROUND)
                lineJoin(LineJoin.ROUND)
                lineOpacity(1.0)
                lineWidth(6.0)
                lineColor(pathsColors[allPaths.indexOf(it) % pathsColors.size])
            }
        }
    }
}

fun getStyleForInit(
    map: MutableState<MapView?>,
    allPaths: List<UiDeliveryPath>
): StyleContract.StyleExtension {

    return style(style = Style.STANDARD) {
        allPaths.forEach { path ->
            val sourceId =
                path.hashCode().toString() + Random.nextInt().toString()
            val layerId = "${sourceId}_layer"

            map.value?.mapboxMap?.addSource(geoJsonSource(sourceId) {
                featureCollection(
                    FeatureCollection.fromJson(
                        Json.encodeToString(path.geoJson)
                    ),
                    "${sourceId}_data"
                )
            })

            map.value?.mapboxMap?.addLayer(
                lineLayer(
                    layerId,
                    sourceId
                ) {
                    lineCap(LineCap.ROUND)
                    lineJoin(LineJoin.ROUND)
                    lineOpacity(1.0)
                    lineWidth(6.0)
                    lineColor(pathsColors[allPaths.indexOf(path) % pathsColors.size])
                })
        }
    }
}