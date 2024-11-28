package com.mtdevelopment.checkout.data.remote.model.response.firestore

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class DataDeliveryPathsResponse(
    @SerialName("id")
    val id: String = "",
    @SerialName("path_name")
    val pathName: String? = null,
    @SerialName("cities")
    val availableCities: List<String>? = null
)
