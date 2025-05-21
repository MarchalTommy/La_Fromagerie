package com.mtdevelopment.admin.presentation.model

import com.mtdevelopment.core.model.Order

data class OrderScreenState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
