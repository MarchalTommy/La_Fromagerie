package com.mtdevelopment.admin.data.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class GoogleComputeBody(
    val origin: WaypointData,
    val destination: WaypointData,
    val intermediates: List<WaypointData>,
    val travelMode: String,
    val routingPreference: String,
    val polylineQuality: String,
    val polylineEncoding: String,
    /*
    Utilise la norme RFC 3339, où la sortie générée est toujours normalisée avec le suffixe Z et
    utilise 0, 3, 6 ou 9 chiffres décimaux. Les décalages autres que "Z" sont également acceptés.
    Exemples: "2014-10-02T15:01:23Z", "2014-10-02T15:01:23.045123456Z" ou "2014-10-02T15:01:23+05:30".
     */
    val departureTime: String? = null,
    val arrivalTime: String? = null,
    val computeAlternativeRoutes: Boolean,
    val routeModifiers: RouteModifiers,
    val languageCode: String,
    val regionCode: String? = null,
    val units: String,
    val optimizeWaypointOrder: Boolean,
    val trafficModel: TrafficModel? = null
)

enum class TrafficModel {
    BEST_GUESS,
    OPTIMISTIC,
    PESSIMISTIC
}

@Serializable
data class RouteModifiers(
    val avoidHighways: Boolean,
    val avoidFerries: Boolean,
    val avoidTolls: Boolean,
    val vehicleInfo: VehicleInfo,
)

@Serializable
data class VehicleInfo(
    val emissionType: String
)


