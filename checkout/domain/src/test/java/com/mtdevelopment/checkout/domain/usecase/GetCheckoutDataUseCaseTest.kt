package com.mtdevelopment.checkout.domain.usecase

import app.cash.turbine.test
import com.mtdevelopment.core.model.CartItem
import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.model.UserInformation
import com.mtdevelopment.core.repository.SharedDatastore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetCheckoutDataUseCaseTest {

    private val user = UserInformation(
        name = "Jane",
        address = "1 rue du Fromage",
        billingAddress = "2 rue de la Facture",
        lastSelectedPath = "Pontarlier"
    )
    private val cart = CartItems(
        cartItems = listOf(
            CartItem(name = "Comté", price = 1000L, quantity = 2),
            CartItem(name = "Morbier", price = 500L, quantity = 1)
        ),
        totalPrice = 2500L
    )

    private fun useCase(
        userFlow: UserInformation? = user,
        cartFlow: CartItems? = cart,
        deliveryDate: Long = 123L
    ): GetCheckoutDataUseCase {
        val datastore = mockk<SharedDatastore> {
            every { userInformationFlow } returns flowOf(userFlow)
            every { cartItemsFlow } returns flowOf(cartFlow)
            every { deliveryDateFlow } returns flowOf(deliveryDate)
        }
        return GetCheckoutDataUseCase(datastore)
    }

    @Test
    fun `combines user cart and delivery date into checkout information`() = runTest {
        useCase().invoke().test {
            val info = awaitItem()
            assertEquals("Jane", info?.buyerName)
            assertEquals("1 rue du Fromage", info?.buyerAddress)
            assertEquals("2 rue de la Facture", info?.billingAddress)
            assertEquals(cart, info?.cartItems)
            assertEquals(123L, info?.deliveryDate)
            awaitComplete()
        }
    }

    @Test
    fun `total price is recomputed from cart line items`() = runTest {
        useCase().invoke().test {
            // 2 x 1000 + 1 x 500
            assertEquals(2500L, awaitItem()?.totalPrice)
            awaitComplete()
        }
    }

    @Test
    fun `emits null when user information is missing`() = runTest {
        useCase(userFlow = null).invoke().test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits null when cart is missing`() = runTest {
        useCase(cartFlow = null).invoke().test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }
}
