package com.mtdevelopment.delivery.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DeliveryPath(
    val id: String,
    val pathName: String,
    val availableCities: List<Pair<String, Int>>,
    val locations: List<Pair<Double, Double>>?,
    val deliveryDay: String,
    val geoJson: GeoJsonFeatureCollection?
)