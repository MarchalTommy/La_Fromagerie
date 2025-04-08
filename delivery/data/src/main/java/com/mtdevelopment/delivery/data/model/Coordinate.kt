package com.mtdevelopment.delivery.data.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Serializable
@Keep
data class Coordinate(
    val latitude: Double,
    val longitude: Double
)