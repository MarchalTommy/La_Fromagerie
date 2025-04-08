package com.mtdevelopment.delivery.data.model.response.firestore

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class DataDeliveryPathsResponse(
    @SerialName("id")
    val id: String = "",
    @SerialName("path_name")
    val path_name: String? = null,
    @SerialName("cities")
    val cities: List<String>? = null,
    @SerialName("delivery_day")
    val deliveryDay: String = "",
    @SerialName("postcodes")
    val postcodes: List<Int>? = null
)