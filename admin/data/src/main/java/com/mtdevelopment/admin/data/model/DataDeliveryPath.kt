package com.mtdevelopment.admin.data.model

import androidx.annotation.Keep
import com.mtdevelopment.core.model.DeliveryPath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object (DTO) for delivery paths, used for Firestore serialization.
 * It separates cities and postcodes into two lists for easier storage/querying in Firestore if needed.
 */
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

/**
 * Maps a domain [DeliveryPath] model to a [DataDeliveryPath] DTO.
 * It transforms the list of pairs (City, Postcode) into two separate lists.
 */
fun DeliveryPath.toDataDeliveryPath() = DataDeliveryPath(
    id = id,
    path_name = pathName,
    delivery_day = deliveryDay,
    cities = availableCities.map { it.first },
    postcodes = availableCities.map { it.second }
)

