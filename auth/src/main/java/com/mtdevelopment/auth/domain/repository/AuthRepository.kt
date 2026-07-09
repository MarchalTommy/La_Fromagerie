package com.mtdevelopment.auth.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Gate around the single-operator admin session, backed by Firebase Auth.
 */
interface AuthRepository {

    /**
     * Snapshot of the current session, read synchronously so the UI can decide on the
     * very first frame whether to show the PIN screen (Firebase persists the signed-in
     * user across launches).
     */
    val isAuthenticated: Boolean

    /**
     * Emits the current authentication state and every subsequent change (sign-in or
     * sign-out). Fires immediately with the current value on collection.
     */
    val authState: Flow<Boolean>

    /**
     * Signs the operator in with [pin] as the password of the fixed admin account.
     * Returns [Result.failure] on a wrong PIN, a disabled provider, or no network.
     */
    suspend fun signInWithPin(pin: String): Result<Unit>

    fun signOut()
}
