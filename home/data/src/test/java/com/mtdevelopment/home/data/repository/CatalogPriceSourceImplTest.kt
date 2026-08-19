package com.mtdevelopment.home.data.repository

import android.util.Log
import com.mtdevelopment.core.model.FulfillmentType
import com.mtdevelopment.core.model.Product
import com.mtdevelopment.core.util.DataResult
import com.mtdevelopment.home.domain.repository.ProductRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CatalogPriceSourceImplTest {

    private val productRepository: ProductRepository = mockk()
    private val source = CatalogPriceSourceImpl(productRepository)

    private val comte = Product(
        id = "1",
        name = "Comté",
        priceInCents = 1000L,
        priceInCentsPickupShop = 900L,
        imageUrl = "",
        type = "CHEESE"
    )

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `resolves the shop price when collecting at the shop`() = runTest {
        coEvery { productRepository.getAllProducts(any()) } returns DataResult.Success(listOf(comte))

        assertEquals(900L, source.unitPriceCents("Comté", FulfillmentType.PICKUP_SHOP))
    }

    @Test
    fun `resolves the delivery price for delivery and market pickup`() = runTest {
        coEvery { productRepository.getAllProducts(any()) } returns DataResult.Success(listOf(comte))

        assertEquals(1000L, source.unitPriceCents("Comté", FulfillmentType.DELIVERY))
        assertEquals(1000L, source.unitPriceCents("Comté", FulfillmentType.PICKUP_MARKET))
    }

    @Test
    fun `returns null when the product is no longer in the catalogue`() = runTest {
        coEvery { productRepository.getAllProducts(any()) } returns DataResult.Success(listOf(comte))

        assertNull(source.unitPriceCents("Morbier", FulfillmentType.DELIVERY))
    }

    @Test
    fun `returns null without throwing when the catalogue read fails`() = runTest {
        coEvery { productRepository.getAllProducts(any()) } returns DataResult.Error(message = "offline")

        assertNull(source.unitPriceCents("Comté", FulfillmentType.DELIVERY))
    }

    @Test
    fun `returns null without throwing when the catalogue read raises`() = runTest {
        coEvery { productRepository.getAllProducts(any()) } throws IllegalStateException("boom")

        assertNull(source.unitPriceCents("Comté", FulfillmentType.DELIVERY))
    }
}
