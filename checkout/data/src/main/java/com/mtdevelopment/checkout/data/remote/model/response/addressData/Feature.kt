package com.mtdevelopment.checkout.data.remote.model.response.addressData

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Feature(
    val geometry: Geometry,
    val properties: Properties,
    val type: String
)