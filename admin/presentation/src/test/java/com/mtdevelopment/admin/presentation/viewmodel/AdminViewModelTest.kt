package com.mtdevelopment.admin.presentation.viewmodel

import android.net.Uri
import com.mtdevelopment.admin.domain.usecase.AddNewPathUseCase
import com.mtdevelopment.admin.domain.usecase.AddNewProductUseCase
import com.mtdevelopment.admin.domain.usecase.DeletePathUseCase
import com.mtdevelopment.admin.domain.usecase.DeleteProductUseCase
import com.mtdevelopment.admin.domain.usecase.GetAllOrdersUseCase
import com.mtdevelopment.admin.domain.usecase.GetCurrentLocationOnceUseCase
import com.mtdevelopment.admin.domain.usecase.GetIsInTrackingModeUseCase
import com.mtdevelopment.admin.domain.usecase.GetOptimizedDeliveryUseCase
import com.mtdevelopment.admin.domain.usecase.AddNewPickupPointUseCase
import com.mtdevelopment.admin.domain.usecase.CancelStaleOnSiteOrdersUseCase
import com.mtdevelopment.admin.domain.usecase.DeletePickupPointUseCase
import com.mtdevelopment.admin.domain.usecase.MarkOrderPaidOnSiteUseCase
import com.mtdevelopment.admin.domain.usecase.GetAllPickupPointsUseCase
import com.mtdevelopment.admin.domain.usecase.GetPreparationStatusesUseCase
import com.mtdevelopment.admin.domain.usecase.UpdatePickupPointUseCase
import com.mtdevelopment.admin.domain.usecase.GetShouldShowBatterieOptimizationUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateDeliveryPathUseCase
import com.mtdevelopment.admin.domain.usecase.UpdatePreparationStatusUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateProductUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateShouldShowBatterieOptimizationUseCase
import com.mtdevelopment.admin.domain.usecase.UploadImageUseCase
import com.mtdevelopment.admin.domain.model.OptimizedRouteWithOrders
import com.mtdevelopment.core.model.DeliveryCity
import com.mtdevelopment.core.model.DeliveryPath
import com.mtdevelopment.core.model.FulfillmentType
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus
import com.mtdevelopment.core.model.Product
import com.mtdevelopment.core.model.ProductType
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.usecase.GetAutocompleteSuggestionsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    private val updateProductUseCase: UpdateProductUseCase = mockk()
    private val deleteProductUseCase: DeleteProductUseCase = mockk(relaxed = true)
    private val addNewProductUseCase: AddNewProductUseCase = mockk()
    private val updateDeliveryPathUseCase: UpdateDeliveryPathUseCase = mockk(relaxed = true)
    private val deletePathUseCase: DeletePathUseCase = mockk(relaxed = true)
    private val addNewPathUseCase: AddNewPathUseCase = mockk(relaxed = true)
    private val getAllOrdersUseCase: GetAllOrdersUseCase = mockk(relaxed = true)
    private val uploadImageUseCase: UploadImageUseCase = mockk()
    private val isInTrackingModeUseCase: GetIsInTrackingModeUseCase = mockk(relaxed = true)
    private val getOptimizedDeliveryUseCase: GetOptimizedDeliveryUseCase = mockk(relaxed = true)
    private val getCurrentLocationOnceUseCase: GetCurrentLocationOnceUseCase =
        mockk(relaxed = true)
    private val getAutocompleteSuggestionsUseCase: GetAutocompleteSuggestionsUseCase = mockk()
    private val updateShouldShowBatterieOptimizationUseCase:
            UpdateShouldShowBatterieOptimizationUseCase = mockk(relaxed = true)
    private val getShouldShowBatterieOptimizationUseCase:
            GetShouldShowBatterieOptimizationUseCase = mockk()
    private val getPreparationStatusesUseCase: GetPreparationStatusesUseCase =
        mockk(relaxed = true)
    private val updatePreparationStatusUseCase: UpdatePreparationStatusUseCase =
        mockk(relaxed = true)
    private val getAllPickupPointsUseCase: GetAllPickupPointsUseCase = mockk(relaxed = true)
    private val addNewPickupPointUseCase: AddNewPickupPointUseCase = mockk(relaxed = true)
    private val updatePickupPointUseCase: UpdatePickupPointUseCase = mockk(relaxed = true)
    private val deletePickupPointUseCase: DeletePickupPointUseCase = mockk(relaxed = true)
    private val markOrderPaidOnSiteUseCase: MarkOrderPaidOnSiteUseCase = mockk(relaxed = true)
    private val cancelStaleOnSiteOrdersUseCase: CancelStaleOnSiteOrdersUseCase =
        mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getShouldShowBatterieOptimizationUseCase.invoke() } returns flowOf(false)

        mockkStatic(Uri::class)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun buildViewModel() = AdminViewModel(
        updateProductUseCase,
        deleteProductUseCase,
        addNewProductUseCase,
        updateDeliveryPathUseCase,
        deletePathUseCase,
        addNewPathUseCase,
        getAllOrdersUseCase,
        uploadImageUseCase,
        isInTrackingModeUseCase,
        getOptimizedDeliveryUseCase,
        getCurrentLocationOnceUseCase,
        getAutocompleteSuggestionsUseCase,
        updateShouldShowBatterieOptimizationUseCase,
        getShouldShowBatterieOptimizationUseCase,
        getPreparationStatusesUseCase,
        updatePreparationStatusUseCase,
        getAllPickupPointsUseCase,
        addNewPickupPointUseCase,
        updatePickupPointUseCase,
        deletePickupPointUseCase,
        markOrderPaidOnSiteUseCase,
        cancelStaleOnSiteOrdersUseCase
    )

    private fun product(imageUrl: String?) = UiProductObject(
        id = "1",
        name = "Comté",
        priceInCents = 1200L,
        imageUrl = imageUrl,
        type = ProductType.FROMAGE,
        description = "",
        isAvailable = true
    )

    private fun mockUri(scheme: String?) {
        every { Uri.parse(any()) } returns mockk<Uri> {
            every { this@mockk.scheme } returns scheme
        }
    }

    @Test
    fun `updateProduct with hosted image skips upload and saves product`() =
        runTest(testDispatcher) {
            mockUri(scheme = "https")
            coEvery { updateProductUseCase.invoke(any(), any(), any()) } answers {
                secondArg<() -> Unit>().invoke()
            }

            val viewModel = buildViewModel()
            var success = false
            var error = false
            viewModel.updateProduct(
                product("https://res.cloudinary.com/img.jpg"),
                onLoading = {},
                onSuccess = { success = true },
                onError = { error = true }
            )
            testScheduler.advanceUntilIdle()

            assertTrue(success)
            assertFalse(error)
            coVerify(exactly = 0) { uploadImageUseCase.invoke(any(), any()) }
        }

    @Test
    fun `updateProduct uploads local image and persists hosted url`() =
        runTest(testDispatcher) {
            mockUri(scheme = "content")
            coEvery { uploadImageUseCase.invoke(any(), any()) } answers {
                secondArg<(Result<String>) -> Unit>()
                    .invoke(Result.success("https://res.cloudinary.com/uploaded.jpg"))
            }
            val savedProduct = slot<Product>()
            coEvery { updateProductUseCase.invoke(capture(savedProduct), any(), any()) } answers {
                secondArg<() -> Unit>().invoke()
            }

            val viewModel = buildViewModel()
            var success = false
            viewModel.updateProduct(
                product("content://media/external/images/1"),
                onLoading = {},
                onSuccess = { success = true },
                onError = {}
            )
            testScheduler.advanceUntilIdle()

            assertTrue(success)
            assertEquals("https://res.cloudinary.com/uploaded.jpg", savedProduct.captured.imageUrl)
        }

    @Test
    fun `updateProduct aborts when local image upload fails`() = runTest(testDispatcher) {
        mockUri(scheme = "content")
        coEvery { uploadImageUseCase.invoke(any(), any()) } answers {
            secondArg<(Result<String>) -> Unit>().invoke(Result.failure(Exception("upload down")))
        }

        val viewModel = buildViewModel()
        var error = false
        var loading: Boolean? = null
        viewModel.updateProduct(
            product("content://media/external/images/1"),
            onLoading = { loading = it },
            onSuccess = {},
            onError = { error = true }
        )
        testScheduler.advanceUntilIdle()

        assertTrue(error)
        assertEquals(false, loading)
        coVerify(exactly = 0) { updateProductUseCase.invoke(any(), any(), any()) }
    }

    @Test
    fun `addNewProduct aborts when local image upload fails`() = runTest(testDispatcher) {
        mockUri(scheme = "file")
        coEvery { uploadImageUseCase.invoke(any(), any()) } answers {
            secondArg<(Result<String>) -> Unit>().invoke(Result.failure(Exception("upload down")))
        }

        val viewModel = buildViewModel()
        var error = false
        viewModel.addNewProduct(
            product("file:///tmp/local.jpg"),
            onLoading = {},
            onSuccess = {},
            onError = { error = true }
        )
        testScheduler.advanceUntilIdle()

        assertTrue(error)
        coVerify(exactly = 0) { addNewProductUseCase.invoke(any(), any(), any()) }
    }

    @Test
    fun `addNewProduct without image saves directly`() = runTest(testDispatcher) {
        coEvery { addNewProductUseCase.invoke(any(), any(), any()) } answers {
            secondArg<() -> Unit>().invoke()
        }

        val viewModel = buildViewModel()
        var success = false
        viewModel.addNewProduct(
            product(imageUrl = null),
            onLoading = {},
            onSuccess = { success = true },
            onError = {}
        )
        testScheduler.advanceUntilIdle()

        assertTrue(success)
        coVerify(exactly = 0) { uploadImageUseCase.invoke(any(), any()) }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Delivery path CRUD — the path editor's only route to Firestore.
    ///////////////////////////////////////////////////////////////////////////

    private fun path() = DeliveryPath(
        id = "path-a",
        pathName = "Parcours A",
        availableCities = listOf(
            DeliveryCity("Levier", 25270),
            DeliveryCity("Boujailles", 25560, listOf("Rue du Moulin"))
        ),
        deliveryDay = "TUESDAY"
    )

    @Test
    fun `addNewDeliveryPath forwards the path and reports success`() = runTest(testDispatcher) {
        val captured = slot<DeliveryPath>()
        coEvery {
            addNewPathUseCase.invoke(capture(captured), any(), any())
        } answers { arg<() -> Unit>(1).invoke() }
        val viewModel = buildViewModel()
        var success = false

        viewModel.addNewDeliveryPath(path(), onSuccess = { success = true }, onFailure = {})
        testScheduler.advanceUntilIdle()

        assertTrue(success)
        // The street restriction must reach the repository intact — it is the whole point of the
        // per-city split.
        assertEquals(listOf("Rue du Moulin"), captured.captured.availableCities[1].streets)
        assertEquals(2, captured.captured.availableCities.size)
    }

    @Test
    fun `addNewDeliveryPath reports failure instead of a silent success`() =
        runTest(testDispatcher) {
            coEvery {
                addNewPathUseCase.invoke(any(), any(), any())
            } answers { arg<(Throwable) -> Unit>(2).invoke(RuntimeException("boom")) }
            val viewModel = buildViewModel()
            var failed = false
            var success = false

            viewModel.addNewDeliveryPath(
                path(),
                onSuccess = { success = true },
                onFailure = { failed = true }
            )
            testScheduler.advanceUntilIdle()

            assertTrue(failed)
            assertFalse(success)
        }

    @Test
    fun `updateDeliveryPath forwards the path and reports success`() = runTest(testDispatcher) {
        val captured = slot<DeliveryPath>()
        coEvery {
            updateDeliveryPathUseCase.invoke(capture(captured), any(), any())
        } answers { arg<() -> Unit>(1).invoke() }
        val viewModel = buildViewModel()
        var success = false

        viewModel.updateDeliveryPath(path(), onSuccess = { success = true }, onFailure = {})
        testScheduler.advanceUntilIdle()

        assertTrue(success)
        assertEquals("path-a", captured.captured.id)
    }

    @Test
    fun `updateDeliveryPath reports failure`() = runTest(testDispatcher) {
        coEvery {
            updateDeliveryPathUseCase.invoke(any(), any(), any())
        } answers { arg<(Throwable) -> Unit>(2).invoke(RuntimeException("boom")) }
        val viewModel = buildViewModel()
        var failed = false

        viewModel.updateDeliveryPath(path(), onSuccess = {}, onFailure = { failed = true })
        testScheduler.advanceUntilIdle()

        assertTrue(failed)
    }

    @Test
    fun `deleteDeliveryPath forwards the path and reports success`() = runTest(testDispatcher) {
        val captured = slot<DeliveryPath>()
        coEvery {
            deletePathUseCase.invoke(capture(captured), any(), any())
        } answers { arg<() -> Unit>(1).invoke() }
        val viewModel = buildViewModel()
        var success = false

        viewModel.deleteDeliveryPath(path(), onSuccess = { success = true }, onFailure = {})
        testScheduler.advanceUntilIdle()

        assertTrue(success)
        assertEquals("path-a", captured.captured.id)
    }

    @Test
    fun `deleteDeliveryPath reports failure`() = runTest(testDispatcher) {
        coEvery {
            deletePathUseCase.invoke(any(), any(), any())
        } answers { arg<(Throwable) -> Unit>(2).invoke(RuntimeException("boom")) }
        val viewModel = buildViewModel()
        var failed = false

        viewModel.deleteDeliveryPath(path(), onSuccess = {}, onFailure = { failed = true })
        testScheduler.advanceUntilIdle()

        assertTrue(failed)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Delivery day route optimisation — pickups must never become a van stop.
    ///////////////////////////////////////////////////////////////////////////

    private fun today() =
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT))

    private fun order(
        id: String,
        fulfillmentType: FulfillmentType,
        deliveryDate: String = today()
    ) = Order(
        id = id,
        customerName = "Client $id",
        customerAddress = "1 rue du Test, 25270 Levier",
        customerBillingAddress = "1 rue du Test, 25270 Levier",
        deliveryDate = deliveryDate,
        orderDate = deliveryDate,
        products = mapOf("Comté" to 1),
        status = OrderStatus.PAID,
        note = null,
        fulfillmentType = fulfillmentType
    )

    private fun seedOrders(orders: List<Order>) {
        coEvery { getAllOrdersUseCase.invoke(any()) } answers {
            firstArg<(List<Order>?) -> Unit>().invoke(orders)
        }
    }

    @Test
    fun `getOptimisedPath leaves today's pickup orders out of the route`() =
        runTest(testDispatcher) {
            val delivery = order("delivery-1", FulfillmentType.DELIVERY)
            val pickup = order("pickup-1", FulfillmentType.PICKUP_MARKET)
            seedOrders(listOf(delivery, pickup))
            val routedOrders = slot<List<Order>>()
            coEvery {
                getOptimizedDeliveryUseCase.invoke(any(), capture(routedOrders), any())
            } returns OptimizedRouteWithOrders(emptyList(), listOf(delivery))

            val viewModel = buildViewModel()
            viewModel.getAllOrders()
            testScheduler.advanceUntilIdle()
            viewModel.getOptimisedPath(listOf(delivery.customerAddress)) {}
            testScheduler.advanceUntilIdle()

            // The addresses the screen passes are already stripped of pickups; the order list
            // must match them one for one or the optimiser silently drops the optimisation.
            assertEquals(listOf("delivery-1"), routedOrders.captured.map { it.id })
        }

    @Test
    fun `getOptimisedPath keeps the pickup orders on the preparation screen`() =
        runTest(testDispatcher) {
            val delivery = order("delivery-1", FulfillmentType.DELIVERY)
            val pickup = order("pickup-1", FulfillmentType.PICKUP_SHOP)
            seedOrders(listOf(delivery, pickup))

            val viewModel = buildViewModel()
            viewModel.getAllOrders()
            testScheduler.advanceUntilIdle()

            // Filtering happens at the optimiser call, not at the source: the shop still has
            // to prepare the pickups.
            assertEquals(
                listOf("delivery-1", "pickup-1"),
                viewModel.orderScreenState.value.orders.map { it.id }
            )
        }
}
