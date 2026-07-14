package com.mtdevelopment.core.model

data class DeliveryPath(
    val id: String,
    val pathName: String,
    val availableCities: List<Pair<String, Int>>,
    val deliveryDay: String,
    val deliveryFrequency: String = "WEEKLY",
    val streets: List<String> = emptyList()
)