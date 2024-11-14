package com.mtdevelopment.home.presentation.state

import com.mtdevelopment.core.presentation.sharedModels.UiProductObject

data class HomeUiState(
    val products: List<UiProductObject> = emptyList(),
    val isLoading: Boolean = false,
    val isAdmin: Boolean = false
)
