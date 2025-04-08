package com.mtdevelopment.admin.presentation.model

import com.mtdevelopment.core.model.Product

// TODO: Check date format, either String or Date ?
data class ClientOrder(
    val id: String,
    val quantityPerProducts: Map<Product, Int>,
    val deliveryDate: String,
    val orderDate: String,
    val clientName: String,
    val clientAddress: String
)
