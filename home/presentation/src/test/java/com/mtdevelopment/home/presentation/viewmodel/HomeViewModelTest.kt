package com.mtdevelopment.home.presentation.viewmodel

import app.cash.turbine.test
import com.mtdevelopment.core.presentation.R
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import com.mtdevelopment.core.util.DataResult
import com.mtdevelopment.core.util.UiText
import com.mtdevelopment.home.domain.usecase.GetAllCheesesUseCase
import com.mtdevelopment.home.domain.usecase.GetAllProductsUseCase
import com.mtdevelopment.home.domain.usecase.GetLastFirestoreDatabaseUpdateUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var viewModel: HomeViewModel
    private val getAllProductsUseCase: GetAllProductsUseCase = mockk()
    private val getAllCheesesUseCase: GetAllCheesesUseCase = mockk()
    private val getLastFirestoreDatabaseUpdateUseCase: GetLastFirestoreDatabaseUpdateUseCase =
        mockk()
    private val getIsNetworkConnectedUseCase: GetIsNetworkConnectedUseCase = mockk()

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getIsNetworkConnectedUseCase.invoke() } returns flowOf(true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init triggers checkAndUpdateDatabase and loads products successfully`() =
        runTest(testDispatcher) {
            // Arrange
            coEvery { getLastFirestoreDatabaseUpdateUseCase.invoke(any(), any()) } answers {
                firstArg<() -> Unit>().invoke()
            }
            coEvery { getAllProductsUseCase.invoke(false) } returns DataResult.Success(emptyList())

            // Act
            viewModel = HomeViewModel(
                getAllProductsUseCase,
                getAllCheesesUseCase,
                getLastFirestoreDatabaseUpdateUseCase,
                getIsNetworkConnectedUseCase
            )

            testScheduler.advanceUntilIdle()

            // Assert
            val state = viewModel.homeUiState.value
            assertEquals(
                emptyList<com.mtdevelopment.core.presentation.sharedModels.UiProductObject>(),
                state.products
            )
            assertEquals(false, state.isLoading)
            assertNull(state.isError)

            coVerify { getLastFirestoreDatabaseUpdateUseCase.invoke(any(), any()) }
            coVerify { getAllProductsUseCase.invoke(false) }
        }

    @Test
    fun `init triggers checkAndUpdateDatabase and fails, updating state with error`() =
        runTest(testDispatcher) {
            // Arrange
            coEvery { getLastFirestoreDatabaseUpdateUseCase.invoke(any(), any()) } answers {
                secondArg<() -> Unit>().invoke()
            }

            // Act
            viewModel = HomeViewModel(
                getAllProductsUseCase,
                getAllCheesesUseCase,
                getLastFirestoreDatabaseUpdateUseCase,
                getIsNetworkConnectedUseCase
            )

            testScheduler.advanceUntilIdle()

            // Assert
            val state = viewModel.homeUiState.value
            assertEquals(false, state.isLoading)
            assertTrue(state.isError is UiText.StringResource)
            assertEquals(
                R.string.error_database_update,
                (state.isError as UiText.StringResource).resId
            )
        }

    @Test
    fun `product loading error with message surfaces it as dynamic string`() =
        runTest(testDispatcher) {
            // Arrange
            coEvery { getLastFirestoreDatabaseUpdateUseCase.invoke(any(), any()) } answers {
                firstArg<() -> Unit>().invoke()
            }
            coEvery { getAllProductsUseCase.invoke(false) } returns
                    DataResult.Error(message = "Chargement des produits impossible")

            // Act
            viewModel = HomeViewModel(
                getAllProductsUseCase,
                getAllCheesesUseCase,
                getLastFirestoreDatabaseUpdateUseCase,
                getIsNetworkConnectedUseCase
            )
            testScheduler.advanceUntilIdle()

            // Assert
            val state = viewModel.homeUiState.value
            assertEquals(false, state.isLoading)
            assertTrue(state.isError is UiText.DynamicString)
            assertEquals(
                "Chargement des produits impossible",
                (state.isError as UiText.DynamicString).value
            )
        }

    @Test
    fun `product loading error without message falls back to generic resource`() =
        runTest(testDispatcher) {
            // Arrange
            coEvery { getLastFirestoreDatabaseUpdateUseCase.invoke(any(), any()) } answers {
                firstArg<() -> Unit>().invoke()
            }
            coEvery { getAllProductsUseCase.invoke(false) } returns DataResult.Error()

            // Act
            viewModel = HomeViewModel(
                getAllProductsUseCase,
                getAllCheesesUseCase,
                getLastFirestoreDatabaseUpdateUseCase,
                getIsNetworkConnectedUseCase
            )
            testScheduler.advanceUntilIdle()

            // Assert
            val state = viewModel.homeUiState.value
            assertTrue(state.isError is UiText.StringResource)
            assertEquals(
                R.string.error_loading_products,
                (state.isError as UiText.StringResource).resId
            )
        }

    @Test
    fun `refreshProducts triggers getAllProducts with forceRefresh`() = runTest(testDispatcher) {
        // Arrange
        coEvery { getLastFirestoreDatabaseUpdateUseCase.invoke(any(), any()) } answers {
            firstArg<() -> Unit>().invoke()
        }
        coEvery { getAllProductsUseCase.invoke(any()) } returns DataResult.Success(emptyList())

        viewModel = HomeViewModel(
            getAllProductsUseCase,
            getAllCheesesUseCase,
            getLastFirestoreDatabaseUpdateUseCase,
            getIsNetworkConnectedUseCase
        )

        testScheduler.advanceUntilIdle()

        // Act
        viewModel.refreshProducts()
        testScheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { getAllProductsUseCase.invoke(true) }
    }

    @Test
    fun `setIsLoading updates homeUiState correctly`() = runTest(testDispatcher) {
        // Arrange
        coEvery { getLastFirestoreDatabaseUpdateUseCase.invoke(any(), any()) } answers {
            firstArg<() -> Unit>().invoke()
        }
        coEvery { getAllProductsUseCase.invoke(any()) } returns DataResult.Success(emptyList())

        viewModel = HomeViewModel(
            getAllProductsUseCase,
            getAllCheesesUseCase,
            getLastFirestoreDatabaseUpdateUseCase,
            getIsNetworkConnectedUseCase
        )

        testScheduler.advanceUntilIdle()

        // Act & Assert
        viewModel.homeUiState.test {
            val initialState = awaitItem()
            assertEquals(false, initialState.isLoading)

            viewModel.setIsLoading(true)
            val loadingState = awaitItem()
            assertEquals(true, loadingState.isLoading)

            viewModel.setIsLoading(false)
            val loadedState = awaitItem()
            assertEquals(false, loadedState.isLoading)
        }
    }
}
