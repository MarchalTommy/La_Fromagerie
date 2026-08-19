package com.mtdevelopment.delivery.data.model.entity

import androidx.room.Entity
import com.mtdevelopment.core.model.DeliveryCity
import com.mtdevelopment.delivery.data.model.Coordinate
import com.mtdevelopment.delivery.domain.model.DeliveryPath
import com.mtdevelopment.delivery.domain.model.GeoJsonFeatureCollection
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Room row for a cached delivery path.
 *
 * [availableCities] holds city → postcode and [cityStreets] holds city → street restriction, kept
 * as a sidecar column so the schema change is a plain additive `ALTER TABLE` (see `MIGRATION_5_6`)
 * and rows cached by an older build survive the upgrade. A city absent from [cityStreets] is
 * covered in full, which is both the common case and the safe default for those older rows: they
 * behave exactly as before until the next Firestore refresh repopulates the restrictions.
 *
 * [cityCoordinates] follows the same sidecar pattern (`MIGRATION_6_7`) and holds city → commune
 * center. It is what lets a cached path be read back without geocoding anything. A city absent from
 * it simply has no stored coordinate and gets looked up on the next refresh, so older rows keep
 * working unchanged.
 *
 * [locations] stays as the positional mirror the map and the route service consume. It is derived
 * from the same coordinates — keeping both is redundant, but `DeliveryPath.locations` is public API
 * for those two consumers and dropping it belongs to its own change.
 */
@Serializable
@Entity(tableName = "paths", primaryKeys = ["id"])
data class PathEntity(
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("cities")
    val availableCities: Map<String, Int> = emptyMap(),
    @SerialName("city_streets")
    val cityStreets: Map<String, List<String>> = emptyMap(),
    @SerialName("city_coordinates")
    val cityCoordinates: Map<String, Coordinate> = emptyMap(),
    @SerialName("locations")
    val locations: List<Coordinate>,
    @SerialName("delivery_day")
    val deliveryDay: String = "",
    @SerialName("delivery_frequency")
    val deliveryFrequency: String = "WEEKLY",
    @SerialName("geojson")
    val geojson: String = ""
)

fun PathEntity.toPath(): DeliveryPath {
    return DeliveryPath(
        id = this.id,
        pathName = this.name,
        cities = this.availableCities.map { (name, postcode) ->
            DeliveryCity(
                name = name,
                postcode = postcode,
                streets = this.cityStreets[name] ?: emptyList(),
                latitude = this.cityCoordinates[name]?.latitude,
                longitude = this.cityCoordinates[name]?.longitude
            )
        },
        locations = locations.map {
            Pair(it.latitude, it.longitude)
        },
        deliveryDay = deliveryDay,
        deliveryFrequency = deliveryFrequency,
        geoJson = if (this.geojson.isNotBlank() && this.geojson != "null") {
            Json.decodeFromString<GeoJsonFeatureCollection>(
                this.geojson
            )
        } else {
            null
        }
    )
}

fun DeliveryPath.toPathEntity(): PathEntity {
    return PathEntity(
        id = this.id,
        name = this.pathName,
        availableCities = this.cities.associate { it.name to it.postcode },
        cityStreets = this.cities
            .filter { it.streets.isNotEmpty() }
            .associate { it.name to it.streets },
        cityCoordinates = this.cities
            .mapNotNull { city ->
                city.location?.let { (lat, lng) -> city.name to Coordinate(lat, lng) }
            }
            .toMap(),
        locations = locations.orEmpty().map {
            Coordinate(
                latitude = it.first,
                longitude = it.second
            )
        },
        deliveryDay = deliveryDay,
        deliveryFrequency = deliveryFrequency,
        geojson = Json.encodeToString(this.geoJson)
    )
}
