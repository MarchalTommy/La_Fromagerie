package com.mtdevelopment.delivery.presentation.model

import com.mtdevelopment.admin.presentation.model.AdminUiDeliveryPath
import com.mtdevelopment.delivery.domain.model.DeliveryPath
import com.mtdevelopment.delivery.domain.model.GeoJsonFeatureCollection

data class UiDeliveryPath(
    val id: String,
    val name: String,
    val cities: List<Pair<String, Int>>,
    val locations: List<Pair<Double, Double>>?,
    val deliveryDay: String,
    val geoJson: GeoJsonFeatureCollection?
)

fun DeliveryPath.toUiDeliveryPath(): UiDeliveryPath {
    return UiDeliveryPath(
        id = this.id,
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