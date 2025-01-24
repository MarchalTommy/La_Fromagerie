package com.mtdevelopment.admin.presentation.model

import com.mtdevelopment.core.model.DeliveryPath

data class AdminUiDeliveryPath(
    val id: String,
    val name: String,
    val cities: List<Pair<String, Int>>,
    val deliveryDay: String,
)

fun AdminUiDeliveryPath.toDomainDeliveryPath() = DeliveryPath(
    id = id,
    pathName = name,
    availableCities = cities,
    deliveryDay = deliveryDay
)