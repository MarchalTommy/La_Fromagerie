package com.mtdevelopment.core.presentation

import androidx.compose.material3.SnackbarDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MainViewModelTest {

    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        viewModel = MainViewModel()
    }

    @Test
    fun `splash cannot be removed initially`() {
        assertFalse(viewModel.canRemoveSplash.value)
    }

    @Test
    fun `setCanRemoveSplash flips state`() {
        viewModel.setCanRemoveSplash()
        assertTrue(viewModel.canRemoveSplash.value)
    }

    @Test
    fun `delivery helper navigation trigger toggles`() {
        assertFalse(viewModel.shouldGoToDeliveryHelper.value)

        viewModel.setShouldGoToDeliveryHelper(true)
        assertTrue(viewModel.shouldGoToDeliveryHelper.value)

        viewModel.setShouldGoToDeliveryHelper(false)
        assertFalse(viewModel.shouldGoToDeliveryHelper.value)
    }

    @Test
    fun `setError by message populates error state`() {
        viewModel.setError(
            msg = "Network unavailable",
            actionLabel = "Retry",
            duration = SnackbarDuration.Long
        )

        val state = viewModel.errorState
        assertEquals("Network unavailable", state.message)
        assertEquals(0, state.code)
        assertEquals("Retry", state.actionLabel)
        assertEquals(SnackbarDuration.Long, state.duration)
        assertTrue(state.shouldShowError)
    }

    @Test
    fun `setError by code defaults message to empty`() {
        viewModel.setError(code = 404)

        val state = viewModel.errorState
        assertEquals("", state.message)
        assertEquals(404, state.code)
        assertTrue(state.shouldShowError)
    }

    @Test
    fun `clearError resets state`() {
        viewModel.setError(msg = "boom")
        viewModel.clearError()

        val state = viewModel.errorState
        assertFalse(state.shouldShowError)
        assertEquals("", state.message)
    }
}
