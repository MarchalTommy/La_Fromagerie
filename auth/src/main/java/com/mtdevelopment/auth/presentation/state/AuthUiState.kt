package com.mtdevelopment.auth.presentation.state

/**
 * Immutable UI state of the PIN lock screen.
 *
 * @property pin The digits entered so far (0..[com.mtdevelopment.auth.domain.AuthConfig.PIN_LENGTH]).
 * @property isLoading A sign-in attempt is in flight.
 * @property isAuthenticated An admin session is active — the gate should let the app through.
 * @property error Human-readable error to show under the dots, or null.
 */
data class AuthUiState(
    val pin: String = "",
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null
)
