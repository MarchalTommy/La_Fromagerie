package com.mtdevelopment.checkout.domain.model

data class DeliveryPath(
    val id: String,
    val pathName: String,
    val availableCities: List<Pair<String, Int>>,
    val locations: List<Pair<Double, Double>>?,
    val deliveryDay: String,
    val geoJson: GeoJsonFeatureCollection?
)