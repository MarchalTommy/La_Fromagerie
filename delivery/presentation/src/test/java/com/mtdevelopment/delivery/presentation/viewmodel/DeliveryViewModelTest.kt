package com.mtdevelopment.delivery.presentation.viewmodel

import com.mtdevelopment.core.model.DeliveryCity
import com.mtdevelopment.core.model.AutoCompleteSuggestion
import com.mtdevelopment.core.model.UserInformation
import com.mtdevelopment.core.usecase.GetAutocompleteSuggestionsUseCase
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import com.mtdevelopment.core.usecase.SaveToDatastoreUseCase
import com.mtdevelopment.delivery.domain.model.DeliveryPath
import com.mtdevelopment.delivery.domain.usecase.DeliveryEligibility
import com.mtdevelopment.delivery.domain.usecase.GetAllDeliveryPathsUseCase
import com.mtdevelopment.delivery.domain.usecase.GetDeliveryPathUseCase
import com.mtdevelopment.delivery.domain.usecase.GetStreetSuggestionsUseCase
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
import org.junit.Assert.assertNull
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
    private val getStreetSuggestionsUseCase: GetStreetSuggestionsUseCase = mockk(relaxed = true)

    private val uiPath = UiDeliveryPath(
        id = "1",
        name = "Tournée du Lundi",
        cities = listOf(DeliveryCity("Pontarlier", 25300)),
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
        getAutocompleteSuggestionsUseCase,
        getStreetSuggestionsUseCase
    )

    @Test
    fun `loadClientData maps delivery paths to ui models and restores user info`() =
        runTest(testDispatcher) {
            val domainPath = DeliveryPath(
                id = "1",
                pathName = "Tournée du Lundi",
                cities = listOf(DeliveryCity("Pontarlier", 25300)),
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

    ///////////////////////////////////////////////////////////////////////////
    // Eligibility verdicts and multi-tournée candidates
    ///////////////////////////////////////////////////////////////////////////

    private val otherPath = uiPath.copy(id = "2", name = "Tournée du Jeudi", deliveryDay = "Jeudi")

    @Test
    fun `a single candidate is selected outright`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()

        viewModel.updateCandidatePaths(listOf(uiPath))

        assertEquals(listOf(uiPath), viewModel.deliveryUiDataState.candidatePaths)
        assertEquals(uiPath, viewModel.deliveryUiDataState.selectedPath)
    }

    /** The date picker is what settles it, so nothing must be preselected on the customer's behalf. */
    @Test
    fun `several candidates leave the path unselected`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()

        viewModel.updateCandidatePaths(listOf(uiPath, otherPath))

        assertEquals(listOf(uiPath, otherPath), viewModel.deliveryUiDataState.candidatePaths)
        assertNull(viewModel.deliveryUiDataState.selectedPath)
    }

    @Test
    fun `several candidates keep a previous pick that is still on offer`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()
            viewModel.updateSelectedPath(otherPath)

            viewModel.updateCandidatePaths(listOf(uiPath, otherPath))

            assertEquals(otherPath, viewModel.deliveryUiDataState.selectedPath)
        }

    @Test
    fun `a previous pick that no longer serves the address is dropped`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.updateSelectedPath(otherPath)

        viewModel.updateCandidatePaths(listOf(uiPath))

        assertEquals(uiPath, viewModel.deliveryUiDataState.selectedPath)
    }

    @Test
    fun `no candidate clears the selection`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        viewModel.updateSelectedPath(uiPath)

        viewModel.updateCandidatePaths(emptyList())

        assertTrue(viewModel.deliveryUiDataState.candidatePaths.isEmpty())
        assertNull(viewModel.deliveryUiDataState.selectedPath)
    }

    @Test
    fun `a deliverable verdict marks the customer as on a path`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()

        viewModel.updateEligibility(DeliveryEligibility.DELIVERABLE)

        val state = viewModel.deliveryUiDataState
        assertTrue(state.userLocationOnPath)
        assertFalse(state.userLocationCloseFromPath)
        assertFalse(state.streetNotCovered)
    }

    /**
     * The uncovered-street verdict shares the support affordance with a near miss but carries its
     * own wording, hence both flags.
     */
    @Test
    fun `an uncovered street offers support with its own wording`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()

        viewModel.updateEligibility(DeliveryEligibility.STREET_NOT_COVERED)

        val state = viewModel.deliveryUiDataState
        assertFalse(state.userLocationOnPath)
        assertTrue(state.userLocationCloseFromPath)
        assertTrue(state.streetNotCovered)
    }

    /** Regression: the flag used to survive a later, deliverable address. */
    @Test
    fun `a deliverable address clears a previous uncovered-street verdict`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()
            viewModel.updateEligibility(DeliveryEligibility.STREET_NOT_COVERED)

            viewModel.updateEligibility(DeliveryEligibility.DELIVERABLE)

            assertFalse(viewModel.deliveryUiDataState.streetNotCovered)
        }

    @Test
    fun `an out-of-range address is neither on a path nor eligible for support`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()

            viewModel.updateEligibility(DeliveryEligibility.NOT_ELIGIBLE)

            val state = viewModel.deliveryUiDataState
            assertFalse(state.userLocationOnPath)
            assertFalse(state.userLocationCloseFromPath)
            assertFalse(state.streetNotCovered)
        }

    /**
     * A pending multi-tournée choice must not read as a validation failure: the name and address
     * are complete, only the path is still open, and the date picker closes it.
     */
    @Test
    fun `saveUserInfo persists while several tournees are still in the running`() =
        runTest(testDispatcher) {
            coEvery { getUserInfoFromDatastoreUseCase.invoke() } returns flowOf(null)
            val viewModel = buildViewModel()
            viewModel.setUserNameFieldText("Jean Test")
            viewModel.setAddressFieldText("1 rue des Tests, 25300 Pontarlier")
            viewModel.updateCandidatePaths(listOf(uiPath, otherPath))
            var errored = false

            viewModel.saveUserInfo(onError = { errored = true })
            testDispatcher.scheduler.advanceUntilIdle()

            assertFalse(errored)
            coVerify { saveToDatastoreUseCase.invoke(userInformation = any()) }
        }

    @Test
    fun `saveUserInfo still refuses when no path serves the address`() = runTest(testDispatcher) {
        coEvery { getUserInfoFromDatastoreUseCase.invoke() } returns flowOf(null)
        val viewModel = buildViewModel()
        viewModel.setUserNameFieldText("Jean Test")
        viewModel.setAddressFieldText("1 rue des Tests, 13001 Marseille")
        var errored = false

        viewModel.saveUserInfo(onError = { errored = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(errored)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Street suggestions (admin path editor)
    ///////////////////////////////////////////////////////////////////////////

    @Test
    fun `searching streets exposes what the address database returns`() = runTest(testDispatcher) {
        coEvery {
            getStreetSuggestionsUseCase.invoke("moul", "Boujailles", 25560)
        } returns listOf("Rue du Moulin", "Rue du Moulin Neuf")
        val viewModel = buildViewModel()

        viewModel.searchStreets("moul", "Boujailles", 25560)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf("Rue du Moulin", "Rue du Moulin Neuf"),
            viewModel.deliveryUiDataState.streetSuggestions
        )
    }

    /** Each keystroke would otherwise fire its own request, with answers landing out of order. */
    @Test
    fun `only the last query of a burst reaches the network`() = runTest(testDispatcher) {
        coEvery { getStreetSuggestionsUseCase.invoke(any(), any(), any()) } returns listOf("Grande Rue")
        val viewModel = buildViewModel()

        viewModel.searchStreets("g", "Boujailles", 25560)
        viewModel.searchStreets("gr", "Boujailles", 25560)
        viewModel.searchStreets("gra", "Boujailles", 25560)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { getStreetSuggestionsUseCase.invoke(any(), any(), any()) }
        coVerify(exactly = 1) { getStreetSuggestionsUseCase.invoke("gra", "Boujailles", 25560) }
    }

    @Test
    fun `a blank query clears the suggestions without calling out`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()

        viewModel.searchStreets("", "Boujailles", 25560)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.deliveryUiDataState.streetSuggestions.isEmpty())
        coVerify(exactly = 0) { getStreetSuggestionsUseCase.invoke(any(), any(), any()) }
    }

    /** Suggestions from the previous city must not leak into the next one. */
    @Test
    fun `clearing drops the current suggestions`() = runTest(testDispatcher) {
        coEvery { getStreetSuggestionsUseCase.invoke(any(), any(), any()) } returns listOf("Grande Rue")
        val viewModel = buildViewModel()
        viewModel.searchStreets("gra", "Boujailles", 25560)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearStreetSuggestions()

        assertTrue(viewModel.deliveryUiDataState.streetSuggestions.isEmpty())
    }

    /** The shop can always type the street by hand, so a lookup failure must stay silent. */
    @Test
    fun `a failing lookup leaves the suggestions empty rather than crashing`() =
        runTest(testDispatcher) {
            coEvery {
                getStreetSuggestionsUseCase.invoke(any(), any(), any())
            } throws RuntimeException("offline")
            val viewModel = buildViewModel()

            viewModel.searchStreets("gra", "Boujailles", 25560)
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.deliveryUiDataState.streetSuggestions.isEmpty())
        }
}
