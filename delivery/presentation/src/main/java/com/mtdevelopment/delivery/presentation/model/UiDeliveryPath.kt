package com.mtdevelopment.delivery.presentation.model

import com.mtdevelopment.admin.presentation.model.AdminUiDeliveryPath

data class UiDeliveryPath(
    val id: String,
    val name: String,
    val cities: List<Pair<String, Int>>,
    val locations: List<Pair<Double, Double>>?,
    val deliveryDay: String,
    val geoJson: com.mtdevelopment.delivery.domain.model.GeoJsonFeatureCollection?
)

fun com.mtdevelopment.delivery.domain.model.DeliveryPath.toUiDeliveryPath(): UiDeliveryPath {
    return UiDeliveryPath(
        id = this.pathName,
        name = this.pathName,
        cities = this.availableCities,
        locations = this.locations,
        deliveryDay = deliveryDay,
        geoJson = geoJson
    )
}

fun UiDeliveryPath.toAdminUiDeliveryPath(): AdminUiDeliveryPath {
    return AdminUiDeliveryPath(
        id = this.id,
        name = this.name,
        cities = this.cities,
        deliveryDay = deliveryDay
    )
}

fun AdminUiDeliveryPath.toUiDeliveryPath(): UiDeliveryPath {
    return UiDeliveryPath(
        id = this.id,
        name = this.name,
        cities = this.cities,
        locations = null,
        deliveryDay = deliveryDay,
        geoJson = null
    )
}