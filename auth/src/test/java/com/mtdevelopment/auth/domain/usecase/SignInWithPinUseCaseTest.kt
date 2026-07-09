package com.mtdevelopment.auth.domain.usecase

import com.mtdevelopment.auth.domain.AuthConfig
import com.mtdevelopment.auth.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInWithPinUseCaseTest {

    private val repository: AuthRepository = mockk()
    private val useCase = SignInWithPinUseCase(repository)

    private val validPin = "1".repeat(AuthConfig.PIN_LENGTH)

    @Test
    fun `delegates to repository for a well-formed pin`() = runTest {
        coEvery { repository.signInWithPin(validPin) } returns Result.success(Unit)

        val result = useCase(validPin)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.signInWithPin(validPin) }
    }

    @Test
    fun `rejects a pin of the wrong length without hitting the repository`() = runTest {
        val result = useCase("1".repeat(AuthConfig.PIN_LENGTH - 1))

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.signInWithPin(any()) }
    }

    @Test
    fun `rejects a non-numeric pin without hitting the repository`() = runTest {
        val nonNumeric = "12345a".take(AuthConfig.PIN_LENGTH).padEnd(AuthConfig.PIN_LENGTH, 'a')

        val result = useCase(nonNumeric)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.signInWithPin(any()) }
    }

    @Test
    fun `surfaces a repository failure`() = runTest {
        coEvery { repository.signInWithPin(validPin) } returns
                Result.failure(IllegalStateException("wrong pin"))

        val result = useCase(validPin)

        assertTrue(result.isFailure)
    }
}
