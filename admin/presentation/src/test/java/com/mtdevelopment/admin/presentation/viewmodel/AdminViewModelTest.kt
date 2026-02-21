package com.mtdevelopment.admin.presentation.viewmodel

import com.mtdevelopment.admin.domain.usecase.*
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val updateProductUseCase: UpdateProductUseCase = mockk()
    private val deleteProductUseCase: DeleteProductUseCase = mockk()
    private val addNewProductUseCase: AddNewProductUseCase = mockk()
    private val updateDeliveryPathUseCase: UpdateDeliveryPathUseCase = mockk()
    private val deleteDeliveryPathUseCase: DeletePathUseCase = mockk()
    private val addNewDeliveryPathUseCase: AddNewPathUseCase = mockk()
    private val getAllOrdersUseCase: GetAllOrdersUseCase = mockk()
    private val uploadImageUseCase: UploadImageUseCase = mockk()
    private val isInTrackingModeUseCase: GetIsInTrackingModeUseCase = mockk()
    private val getOptimizedDeliveryUseCase: GetOptimizedDeliveryUseCase = mockk()
    private val shouldShowBatterieOptimizationUseCase: UpdateShouldShowBatterieOptimizationUseCase = mockk()
    private val getShouldShowBatterieOptimizationUseCase: GetShouldShowBatterieOptimizationUseCase = mockk()
    private val getPreparationStatusesUseCase: GetPreparationStatusesUseCase = mockk()
    private val updatePreparationStatusUseCase: UpdatePreparationStatusUseCase = mockk()

    private lateinit var viewModel: AdminViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { getShouldShowBatterieOptimizationUseCase.invoke() } returns flowOf(false)
        every { getPreparationStatusesUseCase.invoke(any()) } returns Unit

        viewModel = AdminViewModel(
            updateProductUseCase,
            deleteProductUseCase,
            addNewProductUseCase,
            updateDeliveryPathUseCase,
            deleteDeliveryPathUseCase,
            addNewDeliveryPathUseCase,
            getAllOrdersUseCase,
            uploadImageUseCase,
            isInTrackingModeUseCase,
            getOptimizedDeliveryUseCase,
            shouldShowBatterieOptimizationUseCase,
            getShouldShowBatterieOptimizationUseCase,
            getPreparationStatusesUseCase,
            updatePreparationStatusUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getAllOrders collects orders from use case and updates state`() = runTest(testDispatcher) {
        // Arrange
        val orders = listOf(
            Order(
                id = "1",
                customerName = "Test User",
                customerAddress = "Test Address",
                customerBillingAddress = "Billing Address",
                deliveryDate = "01/01/2023",
                orderDate = "01/01/2023",
                products = emptyMap(),
                status = OrderStatus.PENDING,
                note = "Test Note"
            )
        )
        every { getAllOrdersUseCase.invoke() } returns flowOf(orders)

        // Act
        viewModel.getAllOrders()
        testScheduler.advanceUntilIdle()

        // Assert
        assertEquals(orders, viewModel.orderScreenState.value.orders)
        assertEquals(false, viewModel.orderScreenState.value.isLoading)
    }
}
