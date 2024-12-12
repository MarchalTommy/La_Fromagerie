package com.mtdevelopment.checkout.data.remote.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Serializable
@Keep
data class Coordinate(
    val latitude: Double,
    val longitude: Double
)