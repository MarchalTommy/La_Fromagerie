package com.mtdevelopment.checkout.presentation.model

import kotlinx.serialization.json.Json
import org.json.JSONObject

data class UiDeliveryPath(
    val id: String,
    val name: String,
    val cities: List<String>,
    val geoJson: JSONObject
)

fun com.mtdevelopment.checkout.domain.model.DeliveryPath.toUiDeliveryPath(): UiDeliveryPath {
    return UiDeliveryPath(
        id = this.pathName,
        name = this.pathName,
        cities = this.availableCities.toList(),
        geoJson = Json.decodeFromString<JSONObject>(geoJson)
    )
}