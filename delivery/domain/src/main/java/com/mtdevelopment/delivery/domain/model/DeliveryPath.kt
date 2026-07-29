package com.mtdevelopment.delivery.domain.model

import com.mtdevelopment.core.model.DeliveryCity
import kotlinx.serialization.Serializable

/**
 * Domain model representing a predefined delivery path.
 *
 * @property id Unique identifier for the path.
 * @property pathName Descriptive name (e.g., "Tournée du Lundi").
 * @property cities Cities covered by this path, each carrying its own optional street restriction.
 * @property locations Center coordinates (Lat, Lng) for each city in the path, used for distance
 *   calculation. Positionally aligned with [cities].
 * @property deliveryDay The day of the week this path is active.
 * @property geoJson Full geographic road data for map visualization (fetched on demand).
 */
@Serializable
data class DeliveryPath(
    val id: String,
    val pathName: String,
    val cities: List<DeliveryCity>,
    val locations: List<Pair<Double, Double>>?,
    val deliveryDay: String,
    val deliveryFrequency: String = "WEEKLY",
    val geoJson: GeoJsonFeatureCollection?
)
