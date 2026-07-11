package com.mtdevelopment.core.usecase

import com.mtdevelopment.core.model.CartItem
import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.model.UserInformation
import com.mtdevelopment.core.repository.SharedDatastore
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SaveToDatastoreUseCaseTest {

    private lateinit var sharedDatastore: SharedDatastore
    private lateinit var useCase: SaveToDatastoreUseCase

    private val cartItems = CartItems(
        cartItems = listOf(CartItem(name = "Comté", price = 1999L, quantity = 2)),
        totalPrice = 3998L
    )
    private val userInformation = UserInformation(
        name = "Jane",
        email = "jane@example.com",
        address = "1 rue du Fromage",
        billingAddress = "1 rue du Fromage",
        lastSelectedPath = "Pontarlier"
    )

    @Before
    fun setUp() {
        sharedDatastore = mockk(relaxed = true)
        useCase = SaveToDatastoreUseCase(sharedDatastore)
    }

    @Test
    fun `invoke with cart items only saves cart items`() = runTest {
        useCase(cartItems = cartItems)

        coVerify(exactly = 1) { sharedDatastore.setCartItems(cartItems) }
        coVerify(exactly = 0) { sharedDatastore.setUserInformation(any()) }
        coVerify(exactly = 0) { sharedDatastore.setDeliveryDate(any()) }
    }

    @Test
    fun `invoke with user information only saves user information`() = runTest {
        useCase(userInformation = userInformation)

        coVerify(exactly = 1) { sharedDatastore.setUserInformation(userInformation) }
        coVerify(exactly = 0) { sharedDatastore.setCartItems(any()) }
        coVerify(exactly = 0) { sharedDatastore.setDeliveryDate(any()) }
    }

    @Test
    fun `invoke with delivery date only saves delivery date`() = runTest {
        useCase(deliveryDate = 123456789L)

        coVerify(exactly = 1) { sharedDatastore.setDeliveryDate(123456789L) }
        coVerify(exactly = 0) { sharedDatastore.setCartItems(any()) }
        coVerify(exactly = 0) { sharedDatastore.setUserInformation(any()) }
    }

    @Test
    fun `invoke with multiple arguments saves them all`() = runTest {
        useCase(
            cartItems = cartItems,
            userInformation = userInformation,
            deliveryDate = 42L
        )

        coVerify(exactly = 1) { sharedDatastore.setCartItems(cartItems) }
        coVerify(exactly = 1) { sharedDatastore.setUserInformation(userInformation) }
        coVerify(exactly = 1) { sharedDatastore.setDeliveryDate(42L) }
    }

    @Test
    fun `invoke without arguments saves nothing`() = runTest {
        useCase()

        coVerify(exactly = 0) { sharedDatastore.setCartItems(any()) }
        coVerify(exactly = 0) { sharedDatastore.setUserInformation(any()) }
        coVerify(exactly = 0) { sharedDatastore.setDeliveryDate(any()) }
    }
}
