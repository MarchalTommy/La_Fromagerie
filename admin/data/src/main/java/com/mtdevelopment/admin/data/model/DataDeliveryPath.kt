package com.mtdevelopment.admin.data.model

import androidx.annotation.Keep
import com.mtdevelopment.core.model.DeliveryPath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class DataDeliveryPath(
    @SerialName("id")
    val id: String = "",
    @SerialName("path_name")
    val path_name: String? = null,
    @SerialName("delivery_day")
    val delivery_day: String = "",
    @SerialName("cities")
    val cities: List<String>? = null,
    @SerialName("postcodes")
    val postcodes: List<Int>? = null
)

fun DeliveryPath.toDataDeliveryPath() = DataDeliveryPath(
    id = id,
    path_name = pathName,
    delivery_day = deliveryDay,
    cities = availableCities.map { it.first },
    postcodes = availableCities.map { it.second }
)

