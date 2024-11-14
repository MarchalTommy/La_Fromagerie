package com.mtdevelopment.core.presentation.sharedModels

import androidx.compose.material3.SnackbarDuration

data class ErrorState(
    val message: String = "",
    val code: Int = 0,
    val action: () -> Unit = {},
    val actionLabel: String? = null,
    val shouldShowError: Boolean = false,
    val duration: SnackbarDuration = SnackbarDuration.Short
)
