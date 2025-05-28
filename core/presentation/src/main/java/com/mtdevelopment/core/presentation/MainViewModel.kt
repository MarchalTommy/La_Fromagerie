package com.mtdevelopment.core.presentation

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mtdevelopment.core.presentation.sharedModels.ErrorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent

class MainViewModel() : ViewModel(), KoinComponent {

    private var _canRemoveSplash: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val canRemoveSplash: StateFlow<Boolean> = _canRemoveSplash.asStateFlow()

    private var _shouldGoToDeliveryHelper: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val shouldGoToDeliveryHelper: StateFlow<Boolean> = _shouldGoToDeliveryHelper.asStateFlow()

    var errorState by mutableStateOf(ErrorState())
        private set

    fun setCanRemoveSplash() {
        _canRemoveSplash.tryEmit(true)
    }

    fun setShouldGoToDeliveryHelper(shouldGo: Boolean) {
        _shouldGoToDeliveryHelper.tryEmit(shouldGo)
    }

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