package com.mtdevelopment.auth.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.mtdevelopment.auth.domain.AuthConfig
import com.mtdevelopment.auth.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase-backed implementation of the admin gate. The PIN is passed straight through
 * as the password of the fixed [AuthConfig.ADMIN_EMAIL] account.
 */
class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override val isAuthenticated: Boolean
        get() = firebaseAuth.currentUser != null

    override val authState: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser != null)
        }
        // Registering fires the listener immediately with the current state.
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithPin(pin: String): Result<Unit> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(AuthConfig.ADMIN_EMAIL, pin).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }
}
