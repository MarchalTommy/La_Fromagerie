package com.mtdevelopment.admin.presentation.model

import com.mtdevelopment.core.model.AutoCompleteSuggestion
import com.mtdevelopment.core.model.Order

data class OrderScreenState(
    val orders: List<Order> = emptyList(),

    val shouldShowDialog: Boolean = false,

    val searchQuery: String = "",
    val suggestions: List<AutoCompleteSuggestion?> = emptyList(),
    val suggestionsLoading: Boolean = false,
    val showSuggestions: Boolean = false,

    val shouldShowBatterieOptimization: Boolean = false,
    val isInTrackingMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
