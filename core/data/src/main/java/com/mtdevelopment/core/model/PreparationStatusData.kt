package com.mtdevelopment.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class PreparationStatusData(
    @SerialName("id")
    val id: String = "",
    @SerialName("date")
    val date: String = "",
    @SerialName("product_name")
    val productName: String = "",
    @SerialName("is_prepared")
    val isPrepared: Boolean = false
)

fun PreparationStatus.toData() = PreparationStatusData(
    id = id,
    date = date,
    productName = productName,
    isPrepared = isPrepared
)

fun PreparationStatusData.toDomain() = PreparationStatus(
    id = id,
    date = date,
    productName = productName,
    isPrepared = isPrepared
)
