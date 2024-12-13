package com.mtdevelopment.checkout.data.remote.model.request

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class OpenRouteRequest(
    @SerialName("coordinates")
    val coordinates: List<List<Double>>
)
