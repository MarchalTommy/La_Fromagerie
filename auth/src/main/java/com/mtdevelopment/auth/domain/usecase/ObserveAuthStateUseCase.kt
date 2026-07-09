package com.mtdevelopment.auth.domain.usecase

import com.mtdevelopment.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

/**
 * Streams whether an admin session is currently active. Exposes the synchronous
 * [currentValue] too, so the UI can seed its first frame without waiting for the flow.
 */
class ObserveAuthStateUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<Boolean> = authRepository.authState

    val currentValue: Boolean
        get() = authRepository.isAuthenticated
}
