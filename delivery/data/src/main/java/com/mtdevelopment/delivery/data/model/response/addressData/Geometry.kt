package com.mtdevelopment.delivery.data.model.response.addressData

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Geometry(
    val coordinates: List<Double>,
    val type: String
)