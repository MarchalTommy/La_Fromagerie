package com.mtdevelopment.admin.presentation.model

import com.mtdevelopment.core.model.Product

data class DeliveryPreparation(
    val id: String,
    val quantityPerProducts: Map<Int, Product>,
    val deliveryDate: String,
)
