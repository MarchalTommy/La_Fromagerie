package com.mtdevelopment.delivery.presentation.composable

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize
import com.mtdevelopment.delivery.presentation.R
import com.mtdevelopment.delivery.presentation.model.UiDeliveryPath
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs

const val DEFAULT_LATITUDE = 46.85 // Approximate center of France
const val DEFAULT_LONGITUDE = 2.35 // Approximate center of France
const val INITIAL_ZOOM_ALL_PATHS = 8.0 // Zoom level when showing all paths
const val INITIAL_ZOOM_NO_PATHS = 4.0 // Zoom level when no paths are available
const val PATH_ZOOM_LEVEL = 9.5
const val USER_LOCATION_ZOOM_LEVEL = 11.0
const val CAMERA_ANIMATION_DURATION = 1500L

const val PATH_SOURCE_ID_PREFIX = "path_source_"
const val PATH_LAYER_ID_PREFIX = "path_layer_"

val pathsColors = listOf(
    "#FF6B6B", "#6BCEFF", "#ff9b54", "#4ECDC4", "#6B6BFF"
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MapBoxComposable(
    modifier: Modifier = Modifier,
    userLocation: Pair<Double, Double>? = null,
    chosenPath: UiDeliveryPath? = null,
    allPaths: List<UiDeliveryPath>? = null,
    isConnectedToInternet: Boolean,
    setIsLoading: (Boolean) -> Unit,
    setColumnScrollingEnabled: (Boolean) -> Unit,
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val screenSize: ScreenSize = rememberScreenSize()

    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    var initialCameraSet by remember { mutableStateOf(false) }

    val shouldShowMarker = remember { mutableStateOf(false) }
    val marker = rememberIconImage(
        key = R.drawable.push_pin_final,
        painter = painterResource(R.drawable.push_pin_final)
    )

    // Calculate the initial camera position based on all paths or default
    val initialCameraOptions = remember(allPaths) {
        calculateInitialCamera(allPaths)
    }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions(initialCameraOptions)
    }

    // --- Effects ---

    // Effect to initialize the map, annotation manager, and listeners
    LaunchedEffect(Unit) {
        setIsLoading(true)
        // The MapEffect below will handle setting isLoading(false) after setup
    }

    // Effect to update paths drawn on the map
    LaunchedEffect(mapViewInstance, chosenPath, allPaths, pathsColors) {
        val mapView = mapViewInstance ?: return@LaunchedEffect
        val style = mapView.mapboxMap.style ?: return@LaunchedEffect

        // Determine which paths to draw
        val pathsToDraw = chosenPath?.let { listOf(it) } ?: allPaths ?: emptyList()

        // Efficiently update sources and layers
        updateMapPaths(style, pathsToDraw, allPaths ?: emptyList(), pathsColors)
    }

    // Effect to handle camera movements based on chosen path or returning to overview
    LaunchedEffect(mapViewportState, chosenPath, allPaths, isConnectedToInternet) {
        if (chosenPath != null) {
            // Calculate bounds for the chosen path
            val bounds = calculateBoundsForPaths(listOf(chosenPath))
            if (bounds != null) {
                // Fly camera to the chosen path
                flyToLocation(mapViewportState, bounds.center, PATH_ZOOM_LEVEL)
            } else if (!isConnectedToInternet) {
                // Fallback for offline mode if bounds calculation fails
                flyToLocation(
                    mapViewportState,
                    Point.fromLngLat(DEFAULT_LONGITUDE, DEFAULT_LATITUDE),
                    INITIAL_ZOOM_ALL_PATHS
                )
            }
        } else {
            // No specific path chosen, fly to overview of all paths or default
            val overviewCamera = calculateInitialCamera(allPaths)
            flyToLocation(mapViewportState, overviewCamera.center, overviewCamera.zoom)
        }
        // Ensure loading is off after camera animation starts or logic completes
        setIsLoading(false)
    }

    // Effect to show user location marker and center map (only when no path is chosen)
    LaunchedEffect(mapViewportState, pointAnnotationManager, userLocation, chosenPath) {
        val manager = pointAnnotationManager ?: return@LaunchedEffect
        manager.deleteAll()

        if (userLocation != null && chosenPath == null) {
            val userPoint = Point.fromLngLat(userLocation.second, userLocation.first)
            // Check for valid coordinates (Mapbox might handle (0,0) but good practice)
            if (abs(userPoint.latitude()) > 0.0001 && abs(userPoint.longitude()) > 0.0001) {
                shouldShowMarker.value = true
                // Fly camera to user location
                flyToLocation(mapViewportState, userPoint, USER_LOCATION_ZOOM_LEVEL)
            }
        } else {
            shouldShowMarker.value = false
        }
    }

    // --- UI ---

    Card(
        modifier = modifier
            .heightIn(min = 150.dp, max = (screenSize.height / 5) * 2)
            .fillMaxWidth()
            .padding(4.dp)
            .focusable(true),
        elevation = CardDefaults.elevatedCardElevation(),
        colors = CardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.primaryContainer),
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.secondaryContainer)
        ),
    ) {
        MapboxMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .pointerInteropFilter {
                    when (it.action) {
                        MotionEvent.ACTION_DOWN -> {
                            setColumnScrollingEnabled(false) // Disable parent scroll on touch
                            false // Indicate event not consumed
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            setColumnScrollingEnabled(true) // Re-enable parent scroll
                            false // Indicate event not consumed
                        }

                        else -> false
                    }
                },
            mapViewportState = mapViewportState,
            scaleBar = { /* Nothing */ },
            attribution = { /* Nothing */ },
            logo = { /* Nothing */ }
        ) {
            if (shouldShowMarker.value) {
                PointAnnotation(
                    point = Point.fromLngLat(
                        userLocation?.second!!,
                        userLocation.first
                    )
                ) {
                    iconImage = marker
                }
            } else {
                mapViewInstance?.annotations?.cleanup()
            }

            MapEffect(Unit) { mapView ->
                mapViewInstance = mapView

                // Initialize annotation manager
                val annotationPlugin = mapView.annotations
                val annotationConfig = AnnotationConfig(
                    // layerId = "map_annotation_layer" // Optional: Specify layer ID if needed
                )
                pointAnnotationManager =
                    annotationPlugin.createPointAnnotationManager(annotationConfig)

                // Setup gesture listener for enabling parent scroll after map move ends
                mapView.gestures.addOnMoveListener(object : OnMoveListener {
                    override fun onMoveBegin(detector: MoveGestureDetector) {}
                    override fun onMove(detector: MoveGestureDetector): Boolean = false
                    override fun onMoveEnd(detector: MoveGestureDetector) {
                        setColumnScrollingEnabled(true) // Re-enable parent scroll
                    }
                })

                // Map is ready, hide initial loader if it hasn't been hidden by camera effects
                setIsLoading(false)
                initialCameraSet = true // Mark initial camera as potentially set
            }
        }
    }
}

// --- Helper Functions ---

/**
 * Calculates the initial camera options based on available paths.
 */
private fun calculateInitialCamera(allPaths: List<UiDeliveryPath>?): CameraOptions {
    val bounds = calculateBoundsForPaths(allPaths)
    return if (bounds != null) {
        CameraOptions.Builder()
            .center(bounds.center)
            .zoom(INITIAL_ZOOM_ALL_PATHS)
            .padding(EdgeInsets(50.0, 50.0, 50.0, 50.0))
            .build()
    } else {
        CameraOptions.Builder()
            .center(Point.fromLngLat(DEFAULT_LONGITUDE, DEFAULT_LATITUDE))
            .zoom(INITIAL_ZOOM_NO_PATHS)
            .build()
    }
}

/**
 * Data class to hold calculated bounds and center.
 */
private data class MapBounds(val northWest: Point, val southEast: Point) {
    val center: Point by lazy {
        Point.fromLngLat(
            (northWest.longitude() + southEast.longitude()) / 2.0,
            (northWest.latitude() + southEast.latitude()) / 2.0
        )
    }
}

/**
 * Calculates the bounding box encompassing all coordinates in the given paths.
 * Returns null if paths are null, empty, or contain no valid locations.
 */
private fun calculateBoundsForPaths(paths: List<UiDeliveryPath>?): MapBounds? {
    val allCoordinates = paths?.flatMap { it.locations ?: emptyList() }
        ?.filter { it.first != 0.0 || it.second != 0.0 } // Filter out invalid points

    if (allCoordinates.isNullOrEmpty()) {
        return null
    }

    val minLat = allCoordinates.minOf { it.first }
    val maxLat = allCoordinates.maxOf { it.first }
    val minLng = allCoordinates.minOf { it.second }
    val maxLng = allCoordinates.maxOf { it.second }

    // Basic validation
    if (minLat == maxLat && minLng == maxLng && allCoordinates.size == 1) {
        // Handle single point case - maybe return a small bounding box around it or just the point
        // For simplicity, returning a slightly expanded bounds might work, or just the center point.
        // Let's return null for now, camera logic can handle centering on a single point.
        // return MapBounds(Point.fromLngLat(minLng, maxLat), Point.fromLngLat(maxLng, minLat)) // Creates a zero-size bound
        return MapBounds(
            Point.fromLngLat(minLng, maxLat),
            Point.fromLngLat(maxLng, minLat)
        ) // Center calculation will work
    }
    if (minLat > maxLat || minLng > maxLng) return null // Invalid bounds


    return MapBounds(
        northWest = Point.fromLngLat(minLng, maxLat),
        southEast = Point.fromLngLat(maxLng, minLat)
    )
}

/**
 * Animates the map camera to a specific point and zoom level.
 */
private fun flyToLocation(
    mapViewportState: MapViewportState,
    center: Point?,
    zoom: Double?
) {
    if (center == null || zoom == null) return // Cannot fly without center/zoom

    mapViewportState.flyTo(
        cameraOptions = CameraOptions.Builder()
            .center(center)
            .zoom(zoom)
            .build(),
        animationOptions = MapAnimationOptions.mapAnimationOptions {
            duration(CAMERA_ANIMATION_DURATION)
        }
    )
}

/**
 * Updates the map style to display the provided paths.
 * Clears old path layers/sources before adding new ones.
 */
private fun updateMapPaths(
    style: Style,
    pathsToDraw: List<UiDeliveryPath>,
    allPaths: List<UiDeliveryPath>, // Needed for consistent color mapping
    colors: List<String>
) {
    // 1. Remove all existing path layers and sources first
    style.styleLayers.filter { it.id.startsWith(PATH_LAYER_ID_PREFIX) }.forEach {
        style.removeStyleLayer(it.id)
    }
    style.styleSources.filter { it.id.startsWith(PATH_SOURCE_ID_PREFIX) }.forEach {
        style.removeStyleSource(it.id)
    }

    // 2. Add sources and layers for the paths to be drawn
    pathsToDraw.forEach { path ->
        // Find the original index in allPaths to maintain consistent coloring
        val originalIndex = allPaths.indexOf(path).takeIf { it != -1 } ?: pathsToDraw.indexOf(path)
        val color = colors[originalIndex % colors.size]
        val sourceId = "$PATH_SOURCE_ID_PREFIX${path.hashCode()}"
        val layerId = "$PATH_LAYER_ID_PREFIX${path.hashCode()}"

        try {
            // Add source
            style.addSource(geoJsonSource(sourceId) {
                featureCollection(FeatureCollection.fromJson(Json.encodeToString(path.geoJson)))
            })

            // Add layer
            style.addLayer(lineLayer(layerId, sourceId) {
                lineCap(LineCap.ROUND)
                lineJoin(LineJoin.ROUND)
                lineOpacity(0.9) // Slightly transparent
                lineWidth(5.0)   // Slightly thinner
                lineColor(color)
            })
        } catch (e: Exception) {
            // Log or handle exceptions during style modification
            Log.e(
                "MapStyleError",
                "Failed to add source/layer for path ${path.hashCode()}: ${e.message}"
            )
            // Optionally remove potentially partially added source/layer
            style.removeStyleLayer(layerId)
            style.removeStyleSource(sourceId)
        }
    }
}
