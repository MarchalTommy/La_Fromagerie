package com.mtdevelopment.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductPricingTest {

    private fun product(shopPrice: Long? = null) = Product(
        id = "1",
        name = "Comté AOP",
        priceInCents = 1200L,
        imageUrl = "",
        type = "FROMAGE",
        priceInCentsPickupShop = shopPrice
    )

    @Test
    fun `delivery and market share the reference price`() {
        val cheese = product(shopPrice = 1100L)

        assertEquals(1200L, cheese.priceFor(FulfillmentType.DELIVERY))
        assertEquals(1200L, cheese.priceFor(FulfillmentType.PICKUP_MARKET))
    }

    @Test
    fun `collecting at the shop uses the shop price when there is one`() {
        assertEquals(1100L, product(shopPrice = 1100L).priceFor(FulfillmentType.PICKUP_SHOP))
    }

    @Test
    fun `a product with no shop price costs the same everywhere`() {
        // The common case, and what every product written before the field existed reads as.
        val cheese = product(shopPrice = null)

        FulfillmentType.entries.forEach { mode ->
            assertEquals(1200L, cheese.priceFor(mode))
        }
    }

    @Test
    fun `switching mode can never raise the price`() {
        // Guaranteed by construction: the admin editor refuses a shop price above the
        // delivery one, so no runtime guard is needed on the client.
        val cheese = product(shopPrice = 1100L)
        val reference = cheese.priceFor(FulfillmentType.DELIVERY)

        FulfillmentType.entries.forEach { mode ->
            assert(cheese.priceFor(mode) <= reference) {
                "$mode charges more than delivery"
            }
        }
    }
}
