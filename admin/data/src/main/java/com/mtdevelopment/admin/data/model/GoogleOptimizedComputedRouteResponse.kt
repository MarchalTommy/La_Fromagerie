package com.mtdevelopment.admin.data.model


import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class GoogleOptimizedComputedRouteResponse(
    @SerialName("routes")
    val routes: List<Route?>? = null
) {
    @Keep
    @Serializable
    data class Route(
        @SerialName("legs")
        val legs: List<Leg?>? = null,
        @SerialName("optimizedIntermediateWaypointIndex")
        val optimizedIntermediateWaypointIndex: List<Int?>? = null
    ) {
        @Keep
        @Serializable
        data class Leg(
            @SerialName("endLocation")
            val endLocation: EndLocation? = null
        ) {
            @Keep
            @Serializable
            data class EndLocation(
                @SerialName("latLng")
                val latLng: LatLng? = null
            ) {
                @Keep
                @Serializable
                data class LatLng(
                    @SerialName("latitude")
                    val latitude: Double? = null,
                    @SerialName("longitude")
                    val longitude: Double? = null
                )
            }
        }
    }
}