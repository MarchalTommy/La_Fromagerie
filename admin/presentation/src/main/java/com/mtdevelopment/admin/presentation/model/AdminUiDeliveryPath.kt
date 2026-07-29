package com.mtdevelopment.admin.presentation.model

import com.mtdevelopment.core.model.DeliveryCity
import com.mtdevelopment.core.model.DeliveryPath

data class AdminUiDeliveryPath(
    val id: String,
    val name: String,
    val cities: List<DeliveryCity>,
    val deliveryDay: String,
    val deliveryFrequency: String = "WEEKLY"
)

fun AdminUiDeliveryPath.toDomainDeliveryPath() = DeliveryPath(
    id = id,
    pathName = name,
    availableCities = cities,
    deliveryDay = deliveryDay,
    deliveryFrequency = deliveryFrequency
)
