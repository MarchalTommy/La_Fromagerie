package com.mtdevelopment.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtdevelopment.auth.domain.AuthConfig
import com.mtdevelopment.auth.domain.usecase.ObserveAuthStateUseCase
import com.mtdevelopment.auth.domain.usecase.SignInWithPinUseCase
import com.mtdevelopment.auth.presentation.state.AuthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the PIN lock screen. Auto-submits once the operator has typed
 * [AuthConfig.PIN_LENGTH] digits, and reflects the Firebase session state so the gate
 * opens as soon as sign-in succeeds (and on a persisted session at launch).
 */
class AuthViewModel(
    private val signInWithPinUseCase: SignInWithPinUseCase,
    observeAuthStateUseCase: ObserveAuthStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(isAuthenticated = observeAuthStateUseCase.currentValue)
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeAuthStateUseCase()
            .onEach { authenticated ->
                _uiState.update { it.copy(isAuthenticated = authenticated) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Called by the keypad. Keeps only digits, caps at the PIN length, clears any
     * previous error, and fires the sign-in attempt when the PIN is complete.
     */
    fun onDigitEntered(digit: Char) {
        if (_uiState.value.isLoading) return
        val next = (_uiState.value.pin + digit)
            .filter(Char::isDigit)
            .take(AuthConfig.PIN_LENGTH)
        _uiState.update { it.copy(pin = next, error = null) }
        if (next.length == AuthConfig.PIN_LENGTH) submit(next)
    }

    fun onDeleteDigit() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(pin = it.pin.dropLast(1), error = null) }
    }

    private fun submit(pin: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            signInWithPinUseCase(pin).fold(
                onSuccess = {
                    // isAuthenticated flips via the auth-state flow; just clear the pad.
                    _uiState.update { it.copy(isLoading = false, pin = "") }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(isLoading = false, pin = "", error = "Code incorrect.")
                    }
                }
            )
        }
    }
}
