package com.mtdevelopment.cart.domain.usecase

import app.cash.turbine.test
import com.mtdevelopment.core.model.CartItem
import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.repository.SharedDatastore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetCartDataUseCaseTest {

    private val datastore: SharedDatastore = mockk()
    private val useCase = GetCartDataUseCase(datastore)

    @Test
    fun `invoke returns flow of cart items from datastore`() = runTest {
        // Arrange
        val expectedCart = CartItems(
            cartItems = listOf(CartItem("Comté", 1000L, 3)),
            totalPrice = 3000L
        )
        every { datastore.cartItemsFlow } returns flowOf(expectedCart)

        // Act & Assert
        useCase.invoke().test {
            val actualCart = awaitItem()
            assertEquals(expectedCart, actualCart)
            awaitComplete()
        }
    }
}
