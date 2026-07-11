package com.mtdevelopment.delivery.presentation.viewmodel

import com.mtdevelopment.core.model.AutoCompleteSuggestion
import com.mtdevelopment.core.model.UserInformation
import com.mtdevelopment.core.usecase.GetAutocompleteSuggestionsUseCase
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import com.mtdevelopment.core.usecase.SaveToDatastoreUseCase
import com.mtdevelopment.delivery.domain.model.DeliveryPath
import com.mtdevelopment.delivery.domain.usecase.GetAllDeliveryPathsUseCase
import com.mtdevelopment.delivery.domain.usecase.GetDeliveryPathUseCase
import com.mtdevelopment.delivery.domain.usecase.GetUserInfoFromDatastoreUseCase
import com.mtdevelopment.delivery.presentation.model.UiDeliveryPath
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeliveryViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    private val getIsConnectedUseCase: GetIsNetworkConnectedUseCase = mockk()
    private val getUserInfoFromDatastoreUseCase: GetUserInfoFromDatastoreUseCase = mockk()
    private val saveToDatastoreUseCase: SaveToDatastoreUseCase = mockk(relaxed = true)
    private val getDeliveryPathUseCase: GetDeliveryPathUseCase = mockk()
    private val getAllDeliveryPathsUseCase: GetAllDeliveryPathsUseCase = mockk()
    private val getAutocompleteSuggestionsUseCase: GetAutocompleteSuggestionsUseCase = mockk()

    private val uiPath = UiDeliveryPath(
        id = "1",
        name = "Tournée du Lundi",
        cities = listOf("Pontarlier" to 25300),
        locations = listOf(46.9 to 6.35),
        deliveryDay = "Lundi",
        geoJson = null
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getIsConnectedUseCase.invoke() } returns flowOf(true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = DeliveryViewModel(
        getIsConnectedUseCase,
        getUserInfoFromDatastoreUseCase,
        saveToDatastoreUseCase,
        getDeliveryPathUseCase,
        getAllDeliveryPathsUseCase,
        getAutocompleteSuggestionsUseCase
    )

    @Test
    fun `loadClientData maps delivery paths to ui models and restores user info`() =
        runTest(testDispatcher) {
            val domainPath = DeliveryPath(
                id = "1",
                pathName = "Tournée du Lundi",
                availableCities = listOf("Pontarlier" to 25300),
                locations = listOf(46.9 to 6.35),
                deliveryDay = "Lundi",
                geoJson = null
            )
            coEvery {
                getAllDeliveryPathsUseCase.invoke(any(), any(), any(), any(), any())
            } answers {
                arg<(List<DeliveryPath?>) -> Unit>(3).invoke(listOf(domainPath, null))
            }
            every { getUserInfoFromDatastoreUseCase.invoke() } returns flowOf(
                UserInformation(
                    name = "Jane",
                    email = "jane@example.com",
                    address = "1 rue du Fromage",
                    billingAddress = "",
                    lastSelectedPath = "Tournée du Lundi"
                )
            )

            val viewModel = buildViewModel()
            viewModel.loadClientData()
            testScheduler.advanceUntilIdle()

            val state = viewModel.deliveryUiDataState
            assertEquals(1, state.deliveryPaths.size)
            assertEquals("Tournée du Lundi", state.deliveryPaths.first().name)
            assertEquals("Jane", state.userNameFieldText)
            assertEquals("1 rue du Fromage", state.deliveryAddressSearchQuery)
            assertEquals("Tournée du Lundi", state.selectedPath?.name)
            assertFalse(state.isLoading)
        }

    @Test
    fun `loadClientData surfaces error when path loading fails`() = runTest(testDispatcher) {
        coEvery {
            getAllDeliveryPathsUseCase.invoke(any(), any(), any(), any(), any())
        } answers {
            arg<() -> Unit>(4).invoke()
        }
        every { getUserInfoFromDatastoreUseCase.invoke() } returns flowOf(null)

        val viewModel = buildViewModel()
        viewModel.loadClientData()
        testScheduler.advanceUntilIdle()

        assertNotNull(viewModel.deliveryUiDataState.isError)
        assertFalse(viewModel.deliveryUiDataState.isLoading)
    }

    @Test
    fun `saveUserInfo persists user information when mandatory fields are filled`() =
        runTest(testDispatcher) {
            every { getUserInfoFromDatastoreUseCase.invoke() } returns flowOf(
                UserInformation(
                    name = "Jane",
                    email = "jane@example.com",
                    address = "1 rue du Fromage",
                    billingAddress = "",
                    lastSelectedPath = "Tournée du Lundi"
                )
            )
            val viewModel = buildViewModel()
            viewModel.setUserNameFieldText("Jane")
            viewModel.setAddressFieldText("1 rue du Fromage")
            viewModel.updateSelectedPath(uiPath)

            var errored = false
            viewModel.saveUserInfo { errored = true }
            testScheduler.advanceUntilIdle()

            assertFalse(errored)
            coVerify(exactly = 1) {
                saveToDatastoreUseCase.invoke(
                    userInformation = UserInformation(
                        name = "Jane",
                        email = "jane@example.com",
                        address = "1 rue du Fromage",
                        billingAddress = "",
                        lastSelectedPath = "Tournée du Lundi"
                    )
                )
            }
        }

    @Test
    fun `saveUserInfo fails fast when no path is selected`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.setAddressFieldText("1 rue du Fromage")

        var errored = false
        viewModel.saveUserInfo { errored = true }
        testScheduler.advanceUntilIdle()

        assertTrue(errored)
        coVerify(exactly = 0) { saveToDatastoreUseCase.invoke(userInformation = any()) }
    }

    @Test
    fun `saveUserInfo fails fast when address is blank`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.updateSelectedPath(uiPath)

        var errored = false
        viewModel.saveUserInfo { errored = true }
        testScheduler.advanceUntilIdle()

        assertTrue(errored)
        coVerify(exactly = 0) { saveToDatastoreUseCase.invoke(userInformation = any()) }
    }

    @Test
    fun `saveSelectedDate persists the date`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()

        viewModel.saveSelectedDate(123456L)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { saveToDatastoreUseCase.invoke(deliveryDate = 123456L) }
    }

    @Test
    fun `onSuggestionSelected fills the field and updates map location`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()

            viewModel.onSuggestionSelected(
                AutoCompleteSuggestion(
                    city = "Pontarlier",
                    postCode = "25300",
                    fulltext = "12 rue de la Gare, Pontarlier",
                    lat = 46.9,
                    long = 6.35
                )
            )
            testScheduler.advanceUntilIdle()

            val state = viewModel.deliveryUiDataState
            assertEquals("12 rue de la Gare, Pontarlier", state.deliveryAddressSearchQuery)
            assertEquals(46.9 to 6.35, state.userCityLocation)
            assertFalse(state.showAddressSuggestions)
        }

    @Test
    fun `setAddressFieldText routes to billing or delivery field`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()

        viewModel.setAddressFieldText("billing street", isBilling = true)
        viewModel.setAddressFieldText("delivery street", isBilling = false)

        assertEquals("billing street", viewModel.deliveryUiDataState.billingAddressSearchQuery)
        assertEquals("delivery street", viewModel.deliveryUiDataState.deliveryAddressSearchQuery)
    }

    @Test
    fun `state toggles update the ui state`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()

        viewModel.setIsDatePickerClickable(true)
        viewModel.setIsDatePickerShown(true)
        viewModel.setDateFieldText("25/12/2025")
        viewModel.setIsBillingDifferent(true)
        viewModel.setColumnScrollingEnabled(false)
        viewModel.updateUserCity("Pontarlier")
        viewModel.updateUserLocationOnPath(true)
        viewModel.updateUserLocationCloseFromPath(true)

        val state = viewModel.deliveryUiDataState
        assertTrue(state.shouldDatePickerBeClickable)
        assertTrue(state.datePickerVisibility)
        assertEquals("25/12/2025", state.dateFieldText)
        assertTrue(state.isBillingDifferent)
        assertFalse(state.columnScrollingEnabled)
        assertEquals("Pontarlier", state.userCity)
        assertTrue(state.userLocationOnPath)
        assertTrue(state.userLocationCloseFromPath)
    }
}
