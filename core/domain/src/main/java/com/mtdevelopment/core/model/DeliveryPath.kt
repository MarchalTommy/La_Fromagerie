package com.mtdevelopment.core.model

data class DeliveryPath(
    val id: String,
    val pathName: String,
    val availableCities: List<DeliveryCity>,
    val deliveryDay: String,
    val deliveryFrequency: String = "WEEKLY"
)
