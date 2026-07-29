package com.mtdevelopment.delivery.presentation.model

import com.mtdevelopment.admin.presentation.model.AdminUiDeliveryPath
import com.mtdevelopment.core.model.DeliveryCity
import com.mtdevelopment.delivery.domain.model.DeliveryPath
import com.mtdevelopment.delivery.domain.model.GeoJsonFeatureCollection

data class UiDeliveryPath(
    val id: String,
    val name: String,
    val cities: List<DeliveryCity>,
    val locations: List<Pair<Double, Double>>?,
    val deliveryDay: String,
    val deliveryFrequency: String = "WEEKLY",
    val geoJson: GeoJsonFeatureCollection?
)

fun DeliveryPath.toUiDeliveryPath(): UiDeliveryPath {
    return UiDeliveryPath(
        id = this.id,
        name = this.pathName,
        cities = this.cities,
        locations = this.locations,
        deliveryDay = deliveryDay,
        deliveryFrequency = deliveryFrequency,
        geoJson = geoJson
    )
}

fun UiDeliveryPath.toDomainDeliveryPath(): DeliveryPath {
    return DeliveryPath(
        id = this.id,
        pathName = this.name,
        cities = this.cities,
        locations = this.locations,
        deliveryDay = deliveryDay,
        deliveryFrequency = deliveryFrequency,
        geoJson = geoJson
    )
}

fun UiDeliveryPath.toAdminUiDeliveryPath(): AdminUiDeliveryPath {
    return AdminUiDeliveryPath(
        id = this.id,
        name = this.name,
        cities = this.cities,
        deliveryDay = deliveryDay,
        deliveryFrequency = deliveryFrequency
    )
}

fun AdminUiDeliveryPath.toUiDeliveryPath(): UiDeliveryPath {
    return UiDeliveryPath(
        id = this.id,
        name = this.name,
        cities = this.cities,
        locations = null,
        deliveryDay = deliveryDay,
        deliveryFrequency = deliveryFrequency,
        geoJson = null
    )
}

fun UiDeliveryPath.getFormattedDeliveryDayAndFrequency(): String {
    return com.mtdevelopment.core.domain.getFormattedDeliveryDayAndFrequency(
        deliveryDay = this.deliveryDay,
        deliveryFrequency = this.deliveryFrequency,
        shortFormat = false
    )
}

fun AdminUiDeliveryPath.getFormattedDeliveryDayAndFrequency(): String {
    return com.mtdevelopment.core.domain.getFormattedDeliveryDayAndFrequency(
        deliveryDay = this.deliveryDay,
        deliveryFrequency = this.deliveryFrequency,
        shortFormat = true
    )
}
