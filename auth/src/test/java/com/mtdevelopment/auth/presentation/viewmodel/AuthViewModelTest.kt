package com.mtdevelopment.auth.presentation.viewmodel

import com.mtdevelopment.auth.domain.AuthConfig
import com.mtdevelopment.auth.domain.usecase.ObserveAuthStateUseCase
import com.mtdevelopment.auth.domain.usecase.SignInWithPinUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    private val signInWithPinUseCase: SignInWithPinUseCase = mockk()
    private val observeAuthStateUseCase: ObserveAuthStateUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { observeAuthStateUseCase.currentValue } returns false
        every { observeAuthStateUseCase.invoke() } returns flowOf(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() =
        AuthViewModel(signInWithPinUseCase, observeAuthStateUseCase)

    private fun typeFullPin(vm: AuthViewModel, pin: String = "1".repeat(AuthConfig.PIN_LENGTH)) {
        pin.forEach { vm.onDigitEntered(it) }
    }

    @Test
    fun `seeds authenticated state from a persisted session`() = runTest(testDispatcher) {
        every { observeAuthStateUseCase.currentValue } returns true
        every { observeAuthStateUseCase.invoke() } returns flowOf(true)

        val vm = buildViewModel()

        assertTrue(vm.uiState.value.isAuthenticated)
    }

    @Test
    fun `entering digits accumulates the pin without submitting early`() =
        runTest(testDispatcher) {
            val vm = buildViewModel()
            testScheduler.advanceUntilIdle()

            vm.onDigitEntered('1')
            vm.onDigitEntered('2')

            assertEquals("12", vm.uiState.value.pin)
            coVerify(exactly = 0) { signInWithPinUseCase.invoke(any()) }
        }

    @Test
    fun `delete removes the last digit`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        testScheduler.advanceUntilIdle()

        vm.onDigitEntered('1')
        vm.onDigitEntered('2')
        vm.onDeleteDigit()

        assertEquals("1", vm.uiState.value.pin)
    }

    @Test
    fun `completing the pin triggers sign-in and clears the pad on success`() =
        runTest(testDispatcher) {
            coEvery { signInWithPinUseCase.invoke(any()) } returns Result.success(Unit)

            val vm = buildViewModel()
            testScheduler.advanceUntilIdle()

            typeFullPin(vm)
            testScheduler.advanceUntilIdle()

            coVerify(exactly = 1) {
                signInWithPinUseCase.invoke("1".repeat(AuthConfig.PIN_LENGTH))
            }
            assertEquals("", vm.uiState.value.pin)
            assertFalse(vm.uiState.value.isLoading)
            assertNull(vm.uiState.value.error)
        }

    @Test
    fun `a wrong pin surfaces an error and resets the pad`() = runTest(testDispatcher) {
        coEvery { signInWithPinUseCase.invoke(any()) } returns
                Result.failure(IllegalStateException("nope"))

        val vm = buildViewModel()
        testScheduler.advanceUntilIdle()

        typeFullPin(vm)
        testScheduler.advanceUntilIdle()

        assertEquals("", vm.uiState.value.pin)
        assertFalse(vm.uiState.value.isLoading)
        assertEquals("Code incorrect.", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isAuthenticated)
    }

    @Test
    fun `auth-state emission flips the gate open`() = runTest(testDispatcher) {
        val authFlow = MutableStateFlow(false)
        every { observeAuthStateUseCase.invoke() } returns authFlow

        val vm = buildViewModel()
        testScheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isAuthenticated)

        authFlow.value = true
        testScheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isAuthenticated)
    }
}
