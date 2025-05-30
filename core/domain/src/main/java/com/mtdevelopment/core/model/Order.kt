package com.mtdevelopment.core.model

data class Order(
    val id: String,
    val customerName: String,
    val customerAddress: String,
    val deliveryDate: String,
    val orderDate: String,
    val products: Map<String, Int>,
    val status: OrderStatus
)