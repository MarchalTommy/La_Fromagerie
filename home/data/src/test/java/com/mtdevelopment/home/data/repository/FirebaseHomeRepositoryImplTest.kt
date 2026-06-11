package com.mtdevelopment.home.data.repository

import com.mtdevelopment.core.model.ProductData
import com.mtdevelopment.core.model.ProductType
import com.mtdevelopment.core.util.DataResult
import com.mtdevelopment.home.data.model.FirestoreUpdateData
import com.mtdevelopment.home.data.source.remote.FirestoreDatabase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FirebaseHomeRepositoryImplTest {

    private lateinit var firestore: FirestoreDatabase
    private lateinit var repository: FirebaseHomeRepositoryImpl

    @Before
    fun setUp() {
        firestore = mockk()
        repository = FirebaseHomeRepositoryImpl(firestore)
    }

    @Test
    fun `getAllProducts maps DTOs to domain and sorts by name`() = runTest {
        coEvery { firestore.getAllProducts() } returns listOf(
            ProductData(id = "2", name = "Morbier", priceCents = 850L, type = ProductType.FROMAGE),
            ProductData(id = "1", name = "Comté", priceCents = 1200L, type = ProductType.FROMAGE)
        )

        val result = repository.getAllProducts() as DataResult.Success

        assertEquals(listOf("Comté", "Morbier"), result.data.map { it.name })
        assertEquals(1200L, result.data.first().priceInCents)
        assertEquals(ProductType.FROMAGE.name, result.data.first().type)
    }

    @Test
    fun `getAllProducts wraps datasource failures in DataResult Error`() = runTest {
        val boom = IllegalStateException("firestore down")
        coEvery { firestore.getAllProducts() } throws boom

        val result = repository.getAllProducts()

        assertTrue(result is DataResult.Error)
        assertEquals(boom, (result as DataResult.Error).exception)
        assertEquals("Chargement des produits impossible", result.message)
    }

    @Test
    fun `getAllCheeses drops null DTOs and sorts by name`() = runTest {
        coEvery { firestore.getAllCheeses() } returns listOf(
            ProductData(id = "2", name = "Morbier"),
            null,
            ProductData(id = "1", name = "Comté")
        )

        val result = repository.getAllCheeses() as DataResult.Success

        assertEquals(listOf("Comté", "Morbier"), result.data.map { it.name })
    }

    @Test
    fun `getAllCheeses wraps datasource failures in DataResult Error`() = runTest {
        coEvery { firestore.getAllCheeses() } throws IllegalStateException("boom")

        assertTrue(repository.getAllCheeses() is DataResult.Error)
    }

    @Test
    fun `getLastDatabaseUpdate maps both timestamps`() = runTest {
        coEvery { firestore.getLastDatabaseUpdate() } returns FirestoreUpdateData(
            productsTimestamp = 111L,
            pathsTimestamp = 222L
        )

        val result = repository.getLastDatabaseUpdate() as DataResult.Success

        assertEquals(111L to 222L, result.data)
    }

    @Test
    fun `getLastDatabaseUpdate wraps datasource failures in DataResult Error`() = runTest {
        coEvery { firestore.getLastDatabaseUpdate() } throws IllegalStateException("boom")

        assertTrue(repository.getLastDatabaseUpdate() is DataResult.Error)
    }
}
