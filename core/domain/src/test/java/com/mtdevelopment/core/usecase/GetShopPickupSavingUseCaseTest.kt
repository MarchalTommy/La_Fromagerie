package com.mtdevelopment.core.usecase

import app.cash.turbine.test
import com.mtdevelopment.core.model.CartItem
import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.model.FulfillmentType
import com.mtdevelopment.core.repository.CatalogPriceSource
import com.mtdevelopment.core.repository.SharedDatastore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetShopPickupSavingUseCaseTest {

    private val datastore: SharedDatastore = mockk()
    private val catalogPriceSource: CatalogPriceSource = mockk()
    private val useCase = GetShopPickupSavingUseCase(datastore, catalogPriceSource)

    private fun priced(name: String, delivered: Long?, collected: Long?) {
        coEvery {
            catalogPriceSource.unitPriceCents(name, FulfillmentType.DELIVERY)
        } returns delivered
        coEvery {
            catalogPriceSource.unitPriceCents(name, FulfillmentType.PICKUP_SHOP)
        } returns collected
    }

    private fun cart(vararg items: CartItem) {
        every { datastore.cartItemsFlow } returns flowOf(
            CartItems(cartItems = items.toList(), totalPrice = 0L)
        )
    }

    @Test
    fun `the saving is the per-unit difference times the quantity`() = runTest {
        cart(CartItem("Comté", 1000L, 3))
        priced("Comté", delivered = 1000L, collected = 900L)

        useCase.invoke().test {
            assertEquals(300L, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `savings add up across lines, and lines priced the same add nothing`() = runTest {
        cart(
            CartItem("Comté", 1000L, 2),
            CartItem("Morbier", 800L, 1),
            CartItem("Bleu", 1200L, 4)
        )
        priced("Comté", delivered = 1000L, collected = 900L)
        priced("Morbier", delivered = 800L, collected = 800L)
        priced("Bleu", delivered = 1200L, collected = 1050L)

        // 2 x 100 + 0 + 4 x 150
        useCase.invoke().test {
            assertEquals(800L, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `an empty basket is worth saying nothing about`() = runTest {
        every { datastore.cartItemsFlow } returns flowOf(null)

        useCase.invoke().test {
            assertEquals(0L, awaitItem())
            awaitComplete()
        }
    }

    /**
     * The customer is being invited to drive to the shop on the strength of this number. A
     * product the catalogue cannot price is left out rather than guessed at from the price
     * stored on the basket line, which was set for whichever mode was in force when it was
     * added and says nothing about the other one.
     */
    @Test
    fun `a product the catalogue cannot price contributes nothing`() = runTest {
        cart(CartItem("Comté", 1000L, 2), CartItem("Fromage retiré", 900L, 1))
        priced("Comté", delivered = 1000L, collected = 900L)
        priced("Fromage retiré", delivered = null, collected = null)

        useCase.invoke().test {
            assertEquals(200L, awaitItem())
            awaitComplete()
        }
    }

    /**
     * The admin editor refuses a shop price above the delivery one, so this cannot be written
     * today — but a document from before that guard would otherwise subtract from the total
     * and quietly turn the banner into a lie.
     */
    @Test
    fun `a shop price above the delivery one never becomes a negative saving`() = runTest {
        cart(CartItem("Comté", 1000L, 2), CartItem("Morbier", 800L, 1))
        priced("Comté", delivered = 1000L, collected = 900L)
        priced("Morbier", delivered = 800L, collected = 1500L)

        useCase.invoke().test {
            assertEquals(200L, awaitItem())
            awaitComplete()
        }
    }
}
