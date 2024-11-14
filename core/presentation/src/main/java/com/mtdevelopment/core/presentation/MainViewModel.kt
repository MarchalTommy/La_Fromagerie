package com.mtdevelopment.core.presentation

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mtdevelopment.core.presentation.sharedModels.ErrorState
import org.koin.core.component.KoinComponent

class MainViewModel(

) : ViewModel(), KoinComponent {

    var errorState by mutableStateOf(ErrorState())
        private set

    fun setError(
        msg: String,
        code: Int? = null,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
        action: (() -> Unit)? = null
    ) {
        errorState = ErrorState(
            msg,
            code ?: 0,
            action ?: {},
            actionLabel,
            shouldShowError = true,
            duration
        )
    }

    fun setError(
        code: Int,
        msg: String? = null,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
        action: (() -> Unit)? = null
    ) {
        errorState = ErrorState(
            msg ?: "",
            code,
            action ?: {},
            actionLabel,
            shouldShowError = true,
            duration
        )
    }

    fun clearError() {
        errorState = ErrorState()
    }
}