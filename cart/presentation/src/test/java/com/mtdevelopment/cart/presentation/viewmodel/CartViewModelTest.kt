package com.mtdevelopment.cart.presentation.viewmodel

import com.mtdevelopment.cart.domain.usecase.GetCartDataUseCase
import com.mtdevelopment.core.model.CartItem
import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.model.ProductType
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import com.mtdevelopment.core.usecase.SaveToDatastoreUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {

    private lateinit var viewModel: CartViewModel
    private val getIsNetworkConnectedUseCase: GetIsNetworkConnectedUseCase = mockk()
    private val saveToDatastoreUseCase: SaveToDatastoreUseCase = mockk()
    private val getCartDataUseCase: GetCartDataUseCase = mockk()

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getIsNetworkConnectedUseCase.invoke() } returns flowOf(true)
        coEvery { saveToDatastoreUseCase.invoke(any()) } returns Unit
        coEvery { getCartDataUseCase.invoke() } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setCartVisibility updates visibility state correctly`() {
        // Arrange
        viewModel = CartViewModel(
            getIsNetworkConnectedUseCase,
            saveToDatastoreUseCase,
            getCartDataUseCase
        )

        // Act
        viewModel.setCartVisibility(true)

        // Assert
        assertEquals(true, viewModel.cartUiState.value.isCartVisible)

        // Act
        viewModel.setCartVisibility(false)

        // Assert
        assertEquals(false, viewModel.cartUiState.value.isCartVisible)
    }

    @Test
    fun `saveClickedItem updates currentItem state correctly`() {
        // Arrange
        viewModel = CartViewModel(
            getIsNetworkConnectedUseCase,
            saveToDatastoreUseCase,
            getCartDataUseCase
        )
        val product = UiProductObject(
            id = "1",
            name = "Comté",
            priceInCents = 10L,
            type = ProductType.FROMAGE,
            isAvailable = true,
            description = "",
            imageUrl = ""
        )

        // Act
        viewModel.saveClickedItem(product)

        // Assert
        assertEquals(product, viewModel.cartUiState.value.currentItem)
    }

    @Test
    fun `addCartObject adds new item with quantity 1`() = runTest(testDispatcher) {
        // Arrange
        viewModel = CartViewModel(
            getIsNetworkConnectedUseCase,
            saveToDatastoreUseCase,
            getCartDataUseCase
        )
        val product = UiProductObject(
            id = "1",
            name = "Comté",
            priceInCents = 10L,
            type = ProductType.FROMAGE,
            isAvailable = true,
            description = "",
            imageUrl = ""
        )

        // Act
        viewModel.addCartObject(valueAsUiObject = product)
        testScheduler.advanceUntilIdle()

        // Assert
        val items = viewModel.cartUiState.value.cartItems?.cartItems
        assertNotNull(items)
        assertEquals(1, items!!.size)
        assertEquals("Comté", items[0]?.name)
        assertEquals(1, items[0]?.quantity)
        assertEquals(10L, viewModel.cartUiState.value.cartItems?.totalPrice)
        coVerify { saveToDatastoreUseCase.invoke(any()) }
    }

    @Test
    fun `addCartObject increments quantity for existing item`() = runTest(testDispatcher) {
        // Arrange
        viewModel = CartViewModel(
            getIsNetworkConnectedUseCase,
            saveToDatastoreUseCase,
            getCartDataUseCase
        )
        val product = UiProductObject(
            id = "1",
            name = "Comté",
            priceInCents = 10L,
            type = ProductType.FROMAGE,
            isAvailable = true,
            description = "",
            imageUrl = ""
        )

        // Act - add first time
        viewModel.addCartObject(valueAsUiObject = product)
        testScheduler.advanceUntilIdle()

        // Act - add second time
        viewModel.addCartObject(valueAsUiObject = product)
        testScheduler.advanceUntilIdle()

        // Assert
        val items = viewModel.cartUiState.value.cartItems?.cartItems
        assertNotNull(items)
        assertEquals(1, items!!.size)
        assertEquals("Comté", items[0]?.name)
        assertEquals(2, items[0]?.quantity)
        assertEquals(20L, viewModel.cartUiState.value.cartItems?.totalPrice)
    }

    @Test
    fun `removeCartObject decrements quantity of existing item`() = runTest(testDispatcher) {
        // Arrange
        viewModel = CartViewModel(
            getIsNetworkConnectedUseCase,
            saveToDatastoreUseCase,
            getCartDataUseCase
        )
        val product = UiProductObject(
            id = "1",
            name = "Comté",
            priceInCents = 10L,
            type = ProductType.FROMAGE,
            isAvailable = true,
            description = "",
            imageUrl = ""
        )

        // Add item twice (quantity = 2)
        viewModel.addCartObject(valueAsUiObject = product)
        testScheduler.advanceUntilIdle()
        viewModel.addCartObject(valueAsUiObject = product)
        testScheduler.advanceUntilIdle()

        val addedItem = viewModel.cartUiState.value.cartItems?.cartItems?.firstOrNull()!!

        // Act - decrement quantity
        viewModel.removeCartObject(addedItem)
        testScheduler.advanceUntilIdle()

        // Assert
        val items = viewModel.cartUiState.value.cartItems?.cartItems
        assertNotNull(items)
        assertEquals(1, items!!.size)
        assertEquals(1, items[0]?.quantity)
        assertEquals(10L, viewModel.cartUiState.value.cartItems?.totalPrice)
    }

    @Test
    fun `totallyRemoveObject removes item completely from cart`() = runTest(testDispatcher) {
        // Arrange
        viewModel = CartViewModel(
            getIsNetworkConnectedUseCase,
            saveToDatastoreUseCase,
            getCartDataUseCase
        )
        val item = CartItem(name = "Comté", price = 10L, quantity = 5)
        viewModel.addCartObject(valueAsCartItem = item)
        testScheduler.advanceUntilIdle()

        // Act
        viewModel.totallyRemoveObject(item)
        testScheduler.advanceUntilIdle()

        // Assert
        val items = viewModel.cartUiState.value.cartItems?.cartItems
        assertNotNull(items)
        assertTrue(items!!.isEmpty())
        assertEquals(0L, viewModel.cartUiState.value.cartItems?.totalPrice)
    }

    @Test
    fun `cart is restored from the DataStore at construction`() = runTest(testDispatcher) {
        // Arrange
        val expectedCartItems = CartItems(
            cartItems = listOf(CartItem(name = "Roquefort", price = 12L, quantity = 2)),
            totalPrice = 24L
        )
        coEvery { getCartDataUseCase.invoke() } returns flowOf(expectedCartItems)

        // Act
        viewModel = CartViewModel(
            getIsNetworkConnectedUseCase,
            saveToDatastoreUseCase,
            getCartDataUseCase
        )
        testScheduler.advanceUntilIdle()

        // Assert
        val items = viewModel.cartUiState.value.cartItems?.cartItems
        assertNotNull(items)
        assertEquals(1, items!!.size)
        assertEquals("Roquefort", items[0]?.name)
        assertEquals(2, items[0]?.quantity)
        assertEquals(24L, viewModel.cartUiState.value.cartItems?.totalPrice)
    }
}
