package com.mtdevelopment.home.data.model

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class FirestoreUpdateData(
    @SerialName("products_timestamp")
    val productsTimestamp: Long = 0L,
    @SerialName("path_timestamp")
    val pathsTimestamp: Long = 0L
)
