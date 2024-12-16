package com.mtdevelopment.admin.presentation.model

data class AdminUiDeliveryPath(
    val id: String,
    val name: String,
    val cities: List<Pair<String, Int>>,
    val deliveryDay: String,
)