package com.mtdevelopment.checkout.domain.usecase

import android.util.Log
import app.cash.turbine.test
import com.mtdevelopment.core.model.CartItem
import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.model.FulfillmentType
import com.mtdevelopment.core.model.UserInformation
import com.mtdevelopment.core.repository.CatalogPriceSource
import com.mtdevelopment.core.repository.SharedDatastore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GetCheckoutDataUseCaseTest {

    private val user = UserInformation(
        name = "Jane",
        email = "jane@example.com",
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

    private val catalogPriceSource: CatalogPriceSource = mockk()
    private lateinit var datastore: SharedDatastore

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        // By default the catalogue restates the stored prices, so the existing expectations
        // describe a catalogue that agrees with the basket.
        coEvery { catalogPriceSource.unitPriceCents("Comté", any()) } returns 1000L
        coEvery { catalogPriceSource.unitPriceCents("Morbier", any()) } returns 500L
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun useCase(
        userFlow: UserInformation? = user,
        cartFlow: CartItems? = cart,
        deliveryDate: Long = 123L,
        mode: String? = FulfillmentType.DELIVERY.name
    ): GetCheckoutDataUseCase {
        datastore = mockk<SharedDatastore> {
            every { userInformationFlow } returns flowOf(userFlow)
            every { cartItemsFlow } returns flowOf(cartFlow)
            every { deliveryDateFlow } returns flowOf(deliveryDate)
            every { fulfillmentTypeFlow } returns flowOf(mode)
        }
        return GetCheckoutDataUseCase(datastore, catalogPriceSource)
    }

    @Test
    fun `combines user cart and delivery date into checkout information`() = runTest {
        useCase().invoke().test {
            val info = awaitItem()
            assertEquals("Jane", info?.buyerName)
            assertEquals("jane@example.com", info?.buyerEmail)
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

    @Test
    fun `charged amount follows the current mode without any rewrite`() = runTest {
        coEvery {
            catalogPriceSource.unitPriceCents("Comté", FulfillmentType.PICKUP_SHOP)
        } returns 900L
        coEvery {
            catalogPriceSource.unitPriceCents("Morbier", FulfillmentType.PICKUP_SHOP)
        } returns 450L

        useCase(mode = FulfillmentType.PICKUP_SHOP.name).invoke().test {
            val info = awaitItem()
            // 2 x 900 + 1 x 450, resolved from the catalogue, not from the stored basket.
            assertEquals(2250L, info?.totalPrice)
            assertEquals(900L, info?.cartItems?.cartItems?.first()?.price)
            awaitComplete()
        }

        coVerify(exactly = 0) { datastore.setCartItems(any()) }
    }

    @Test
    fun `switching from delivery to shop pickup changes the total on the next read`() = runTest {
        val mode = MutableStateFlow(FulfillmentType.DELIVERY.name)
        val datastore = mockk<SharedDatastore> {
            every { userInformationFlow } returns flowOf(user)
            every { cartItemsFlow } returns flowOf(cart)
            every { deliveryDateFlow } returns flowOf(123L)
            every { fulfillmentTypeFlow } returns mode
        }
        coEvery {
            catalogPriceSource.unitPriceCents("Comté", FulfillmentType.PICKUP_SHOP)
        } returns 900L
        coEvery {
            catalogPriceSource.unitPriceCents("Morbier", FulfillmentType.PICKUP_SHOP)
        } returns 450L

        GetCheckoutDataUseCase(datastore, catalogPriceSource).invoke().test {
            assertEquals(2500L, awaitItem()?.totalPrice)

            mode.value = FulfillmentType.PICKUP_SHOP.name

            assertEquals(2250L, awaitItem()?.totalPrice)
        }
    }

    @Test
    fun `product gone from the catalogue keeps its line at the stored price`() = runTest {
        coEvery { catalogPriceSource.unitPriceCents("Morbier", any()) } returns null

        useCase().invoke().test {
            val info = awaitItem()
            assertEquals(2, info?.cartItems?.cartItems?.size)
            assertEquals(500L, info?.cartItems?.cartItems?.get(1)?.price)
            assertEquals(2500L, info?.totalPrice)
            awaitComplete()
        }

        verify { Log.w(any(), any<String>()) }
    }

    @Test
    fun `failed catalogue read falls back to stored prices and logs a warning`() = runTest {
        coEvery { catalogPriceSource.unitPriceCents(any(), any()) } returns null

        useCase().invoke().test {
            // Exactly the amount the previous implementation would have charged: the fallback
            // must never cost the customer the ability to pay.
            assertEquals(2500L, awaitItem()?.totalPrice)
            awaitComplete()
        }

        verify { Log.w(any(), any<String>()) }
    }

    @Test
    fun `a mode priced the same as delivery leaves the total untouched`() = runTest {
        useCase(mode = FulfillmentType.PICKUP_MARKET.name).invoke().test {
            val info = awaitItem()
            assertEquals(2500L, info?.totalPrice)
            assertEquals(cart, info?.cartItems)
            awaitComplete()
        }
    }
}
