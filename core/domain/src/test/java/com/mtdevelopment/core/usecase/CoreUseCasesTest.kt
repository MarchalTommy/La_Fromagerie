package com.mtdevelopment.core.usecase

import app.cash.turbine.test
import com.mtdevelopment.core.model.AutoCompleteSuggestion
import com.mtdevelopment.core.repository.AutocompleteRepository
import com.mtdevelopment.core.repository.NetworkRepository
import com.mtdevelopment.core.repository.SharedDatastore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreUseCasesTest {

    @Test
    fun `ClearCartUseCase clears cart items`() = runTest {
        val datastore = mockk<SharedDatastore>(relaxed = true)
        ClearCartUseCase(datastore).invoke()
        coVerify(exactly = 1) { datastore.clearCartItems() }
    }

    @Test
    fun `ClearOrderUseCase clears order`() = runTest {
        val datastore = mockk<SharedDatastore>(relaxed = true)
        ClearOrderUseCase(datastore).invoke()
        coVerify(exactly = 1) { datastore.clearOrder() }
    }

    @Test
    fun `ClearDatastoreUseCase clears everything`() = runTest {
        val datastore = mockk<SharedDatastore>(relaxed = true)
        ClearDatastoreUseCase(datastore).invoke()
        coVerify(exactly = 1) { datastore.clearAllDatastore() }
    }

    @Test
    fun `GetIsNetworkConnectedUseCase exposes repository flow`() = runTest {
        val repository = mockk<NetworkRepository> {
            every { isConnected } returns flowOf(true, false)
        }

        GetIsNetworkConnectedUseCase(repository).invoke().test {
            assertTrue(awaitItem())
            assertFalse(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `GetAutocompleteSuggestionsUseCase delegates to repository`() = runTest {
        val suggestions = listOf(
            AutoCompleteSuggestion(city = "Pontarlier", postCode = "25300")
        )
        val repository = mockk<AutocompleteRepository> {
            coEvery { getAutoCompleteSuggestions("pont") } returns suggestions
        }

        val result = GetAutocompleteSuggestionsUseCase(repository).invoke("pont")

        assertEquals(suggestions, result)
        coVerify(exactly = 1) { repository.getAutoCompleteSuggestions("pont") }
    }
}
