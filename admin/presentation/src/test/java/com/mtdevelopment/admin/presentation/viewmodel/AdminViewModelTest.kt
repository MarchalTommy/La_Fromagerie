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
import com.mtdevelopment.admin.domain.usecase.GetPreparationStatusesUseCase
import com.mtdevelopment.admin.domain.usecase.GetShouldShowBatterieOptimizationUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateDeliveryPathUseCase
import com.mtdevelopment.admin.domain.usecase.UpdatePreparationStatusUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateProductUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateShouldShowBatterieOptimizationUseCase
import com.mtdevelopment.admin.domain.usecase.UploadImageUseCase
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
        updatePreparationStatusUseCase
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
}
