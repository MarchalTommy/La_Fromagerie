package com.mtdevelopment.auth.domain.usecase

import com.mtdevelopment.auth.domain.AuthConfig
import com.mtdevelopment.auth.domain.repository.AuthRepository

/**
 * Validates the PIN shape locally (right length, digits only) before spending a
 * Firebase sign-in attempt on it, then delegates to the repository.
 */
class SignInWithPinUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(pin: String): Result<Unit> {
        if (pin.length != AuthConfig.PIN_LENGTH || !pin.all { it.isDigit() }) {
            return Result.failure(
                IllegalArgumentException("Le code doit comporter ${AuthConfig.PIN_LENGTH} chiffres.")
            )
        }
        return authRepository.signInWithPin(pin)
    }
}
