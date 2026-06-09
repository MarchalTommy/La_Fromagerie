package com.mtdevelopment.delivery.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain model representing a predefined delivery path.
 * 
 * @property id Unique identifier for the path.
 * @property pathName Descriptive name (e.g., "Tournée du Lundi").
 * @property availableCities List of (City Name, Zip Code) pairs covered by this path.
 * @property locations Center coordinates (Lat, Lng) for each city in the path, used for distance calculation.
 * @property deliveryDay The day of the week this path is active.
 * @property streets Optional list of specific streets covered. If empty, the entire city is assumed deliverable.
 * @property geoJson Full geographic road data for map visualization (fetched on demand).
 */
@Serializable
data class DeliveryPath(
    val id: String,
    val pathName: String,
    val availableCities: List<Pair<String, Int>>,
    val locations: List<Pair<Double, Double>>?,
    val deliveryDay: String,
    val streets: List<String> = emptyList(),
    val geoJson: GeoJsonFeatureCollection?
)