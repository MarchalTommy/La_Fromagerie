package com.mtdevelopment.checkout.data.remote.model.entity

import androidx.room.Entity
import com.mtdevelopment.checkout.data.remote.model.Coordinate
import com.mtdevelopment.checkout.domain.model.DeliveryPath
import com.mtdevelopment.checkout.domain.model.GeoJsonFeatureCollection
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
    val availableCities: List<String> = listOf(),
    @SerialName("locations")
    val locations: List<Coordinate>,
    @SerialName("geojson")
    val geojson: String = ""
)

fun PathEntity.toPath(): DeliveryPath {
    return DeliveryPath(
        id = this.id,
        pathName = this.name,
        availableCities = this.availableCities,
        locations = locations.map {
            Pair(it.latitude, it.longitude)
        },
        geoJson = Json.decodeFromString<GeoJsonFeatureCollection>(this.geojson)
    )
}

fun DeliveryPath.toPathEntity(): PathEntity {
    return PathEntity(
        id = this.id,
        name = this.pathName,
        availableCities = this.availableCities,
        locations = locations!!.map {
            Coordinate(
                latitude = it.first,
                longitude = it.second
            )
        },
        geojson = Json.encodeToString(this.geoJson)
    )
}