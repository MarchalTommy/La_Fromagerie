package com.mtdevelopment.checkout.domain.model

data class DeliveryPath(
    val id: String,
    val pathName: String,
    val availableCities: List<String>,
    val locations: List<Pair<Double, Double>>?,
    val geoJson: GeoJsonFeatureCollection?
)
