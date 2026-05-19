package com.mtdevelopment.home.presentation.state

import com.mtdevelopment.core.presentation.sharedModels.UiProductObject

/**
 * UI State for the Home screen.
 * @property products The list of product objects to display in the catalog.
 * @property isLoading True when a background task (like synchronization) is in progress.
 * @property isError Contains an error message if an operation fails, otherwise null.
 */
data class HomeUiState(
    val products: List<UiProductObject> = emptyList(),
    val isLoading: Boolean = false,
    val isError: String? = null
)
