package com.mtdevelopment.checkout.presentation.model

import com.mtdevelopment.checkout.domain.model.GeoJsonFeatureCollection

data class UiDeliveryPath(
    val id: String,
    val name: String,
    val cities: List<String>,
    val locations: List<Pair<Double, Double>>?,
    val geoJson: GeoJsonFeatureCollection?
)

fun com.mtdevelopment.checkout.domain.model.DeliveryPath.toUiDeliveryPath(): UiDeliveryPath {
    return UiDeliveryPath(
        id = this.pathName,
        name = this.pathName,
        cities = this.availableCities,
        locations = this.locations,
        geoJson = geoJson
    )
}