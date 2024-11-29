package com.mtdevelopment.checkout.presentation.model

import com.mapbox.geojson.GeoJson
import com.mapbox.geojson.gson.GeometryGeoJson

data class UiDeliveryPath(
    val id: String,
    val name: String,
    val cities: List<String>,
    val geoJson: GeoJson
)

fun com.mtdevelopment.checkout.domain.model.DeliveryPath.toUiDeliveryPath(): UiDeliveryPath {
    return UiDeliveryPath(
        id = this.pathName,
        name = this.pathName,
        cities = this.availableCities.toList(),
        geoJson = GeometryGeoJson.fromJson(this.geoJson)
    )
}