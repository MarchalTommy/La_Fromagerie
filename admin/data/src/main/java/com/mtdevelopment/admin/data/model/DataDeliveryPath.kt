package com.mtdevelopment.admin.data.model

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class DataDeliveryPath(
    @SerialName("id")
    val id: String = "",
    @SerialName("path_name")
    val pathName: String? = null,
    @SerialName("cities")
    val availableCities: List<String>? = null
)
