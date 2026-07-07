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

/**
 * App-wide ViewModel that manages global state and events not tied to a specific feature.
 * This includes:
 * 1. Splash Screen lifecycle: Coordination between content loading and splash removal.
 * 2. Navigation events: Global triggers for specific screens (e.g., Delivery Helper).
 * 3. Global Error Handling: Unified state for displaying errors via Snackbars or overlays.
 */
class MainViewModel() : ViewModel(), KoinComponent {

    /**
     * Internal state for splash screen visibility.
     */
    private var _canRemoveSplash: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val canRemoveSplash: StateFlow<Boolean> = _canRemoveSplash.asStateFlow()

    /**
     * Trigger for navigating to the delivery helper (Admin mode).
     */
    private var _shouldGoToDeliveryHelper: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val shouldGoToDeliveryHelper: StateFlow<Boolean> = _shouldGoToDeliveryHelper.asStateFlow()

    /**
     * Trigger for SumUp Hosted Checkout callback.
     */
    private val _sumUpCallbackTrigger: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val sumUpCallbackTrigger: StateFlow<Boolean> = _sumUpCallbackTrigger.asStateFlow()

    fun triggerSumUpCallback() {
        _sumUpCallbackTrigger.tryEmit(true)
    }

    fun clearSumUpCallback() {
        _sumUpCallbackTrigger.tryEmit(false)
    }

    /**
     * Global error state displayed to the user.
     */
    var errorState by mutableStateOf(ErrorState())
        private set

    /**
     * Signals that the app's initial content is loaded and the splash screen can be hidden.
     */
    fun setCanRemoveSplash() {
        _canRemoveSplash.tryEmit(true)
    }

    /**
     * Triggers navigation to the delivery helper screen.
     */
    fun setShouldGoToDeliveryHelper(shouldGo: Boolean) {
        _shouldGoToDeliveryHelper.tryEmit(shouldGo)
    }

    /**
     * Sets a global error with a custom message and optional action.
     */
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

    /**
     * Overload to set a global error primarily by error code.
     */
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

    /**
     * Clears the current global error state.
     */
    fun clearError() {
        errorState = ErrorState()
    }
}