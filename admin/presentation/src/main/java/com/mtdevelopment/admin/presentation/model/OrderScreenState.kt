package com.mtdevelopment.admin.presentation.model

import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.PreparationStatus

data class OrderScreenState(
    val orders: List<Order> = emptyList(),
    val preparationStatuses: List<PreparationStatus> = emptyList(),
    val shouldShowBatterieOptimization: Boolean = false,
    val isInTrackingMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
