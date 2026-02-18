package com.mtdevelopment.core.model

import com.google.firebase.firestore.PropertyName

data class PreparationStatusData(
    @get:PropertyName("id") val id: String = "",
    @get:PropertyName("date") val date: String = "",
    @get:PropertyName("product_name") val productName: String = "",
    @get:PropertyName("is_prepared") val isPrepared: Boolean = false
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
