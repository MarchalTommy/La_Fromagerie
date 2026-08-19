package com.mtdevelopment.cart.domain.usecase

import android.util.Log
import app.cash.turbine.test
import com.mtdevelopment.core.model.CartItem
import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.model.FulfillmentType
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
import org.junit.Before
import org.junit.Test

class GetCartDataUseCaseTest {

    private val datastore: SharedDatastore = mockk()
    private val catalogPriceSource: CatalogPriceSource = mockk()
    private val useCase = GetCartDataUseCase(datastore, catalogPriceSource)

    private val storedCart = CartItems(
        cartItems = listOf(CartItem("Comté", 1000L, 3)),
        totalPrice = 3000L
    )

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `cart is valued at the current mode price without any rewrite`() = runTest {
        every { datastore.cartItemsFlow } returns flowOf(storedCart)
        every { datastore.fulfillmentTypeFlow } returns flowOf(FulfillmentType.PICKUP_SHOP.name)
        coEvery {
            catalogPriceSource.unitPriceCents("Comté", FulfillmentType.PICKUP_SHOP)
        } returns 900L

        useCase.invoke().test {
            val cart = awaitItem()
            assertEquals(900L, cart?.cartItems?.first()?.price)
            assertEquals(2700L, cart?.totalPrice)
            awaitComplete()
        }

        // The stored basket is never rewritten: the price is resolved on read, so there is no
        // derived copy anywhere that could go stale.
        coVerify(exactly = 0) { datastore.setCartItems(any()) }
    }

    @Test
    fun `switching from delivery to shop pickup changes the total on the next read`() = runTest {
        val mode = MutableStateFlow(FulfillmentType.DELIVERY.name)
        every { datastore.cartItemsFlow } returns flowOf(storedCart)
        every { datastore.fulfillmentTypeFlow } returns mode
        coEvery {
            catalogPriceSource.unitPriceCents("Comté", FulfillmentType.DELIVERY)
        } returns 1000L
        coEvery {
            catalogPriceSource.unitPriceCents("Comté", FulfillmentType.PICKUP_SHOP)
        } returns 900L

        useCase.invoke().test {
            assertEquals(3000L, awaitItem()?.totalPrice)

            mode.value = FulfillmentType.PICKUP_SHOP.name

            assertEquals(2700L, awaitItem()?.totalPrice)
        }

        coVerify(exactly = 0) { datastore.setCartItems(any()) }
    }

    @Test
    fun `product gone from the catalogue keeps its line at the stored price`() = runTest {
        every { datastore.cartItemsFlow } returns flowOf(storedCart)
        every { datastore.fulfillmentTypeFlow } returns flowOf(FulfillmentType.PICKUP_SHOP.name)
        coEvery { catalogPriceSource.unitPriceCents("Comté", any()) } returns null

        useCase.invoke().test {
            val cart = awaitItem()
            assertEquals(1, cart?.cartItems?.size)
            assertEquals(1000L, cart?.cartItems?.first()?.price)
            assertEquals(3000L, cart?.totalPrice)
            awaitComplete()
        }

        verify { Log.w(any(), any<String>()) }
    }

    @Test
    fun `failed catalogue read falls back to stored prices and logs a warning`() = runTest {
        every { datastore.cartItemsFlow } returns flowOf(storedCart)
        every { datastore.fulfillmentTypeFlow } returns flowOf(FulfillmentType.PICKUP_SHOP.name)
        // A read failure reaches the use case as a null price, exactly like a missing product:
        // the basket must still be payable.
        coEvery { catalogPriceSource.unitPriceCents(any(), any()) } returns null

        useCase.invoke().test {
            assertEquals(3000L, awaitItem()?.totalPrice)
            awaitComplete()
        }

        verify { Log.w(any(), any<String>()) }
    }

    @Test
    fun `a mode priced the same as delivery leaves the cart untouched`() = runTest {
        every { datastore.cartItemsFlow } returns flowOf(storedCart)
        every { datastore.fulfillmentTypeFlow } returns flowOf(FulfillmentType.PICKUP_MARKET.name)
        coEvery {
            catalogPriceSource.unitPriceCents("Comté", FulfillmentType.PICKUP_MARKET)
        } returns 1000L

        useCase.invoke().test {
            assertEquals(storedCart, awaitItem())
            awaitComplete()
        }

        coVerify(exactly = 0) { datastore.setCartItems(any()) }
    }
}
