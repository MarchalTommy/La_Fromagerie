package com.mtdevelopment.delivery.data.model.entity

import androidx.room.Entity
import com.mtdevelopment.delivery.data.model.Coordinate
import com.mtdevelopment.delivery.domain.model.DeliveryPath
import com.mtdevelopment.delivery.domain.model.GeoJsonFeatureCollection
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
@Entity(tableName = "paths", primaryKeys = ["id"])
data class PathEntity(
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("cities")
    val availableCities: Map<String, Int> = emptyMap(),
    @SerialName("locations")
    val locations: List<Coordinate>,
    @SerialName("delivery_day")
    val deliveryDay: String = "",
    @SerialName("geojson")
    val geojson: String = ""
)

fun PathEntity.toPath(): DeliveryPath {
    return DeliveryPath(
        id = this.id,
        pathName = this.name,
        availableCities = this.availableCities.map { Pair(it.key, it.value) },
        locations = locations.map {
            Pair(it.latitude, it.longitude)
        },
        deliveryDay = deliveryDay,
        geoJson = if (this.geojson != null && this.geojson != "null") {
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
        availableCities = this.availableCities.toMap(),
        locations = locations!!.map {
            Coordinate(
                latitude = it.first,
                longitude = it.second
            )
        },
        deliveryDay = deliveryDay,
        geojson = Json.encodeToString(this.geoJson)
    )
}