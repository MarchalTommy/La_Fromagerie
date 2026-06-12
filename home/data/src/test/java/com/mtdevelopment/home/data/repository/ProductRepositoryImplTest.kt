package com.mtdevelopment.home.data.repository

import com.mtdevelopment.core.model.Product
import com.mtdevelopment.core.repository.SharedDatastore
import com.mtdevelopment.core.util.DataResult
import com.mtdevelopment.home.domain.repository.FirebaseHomeRepository
import com.mtdevelopment.home.domain.repository.RoomHomeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProductRepositoryImplTest {

    private lateinit var firebaseRepository: FirebaseHomeRepository
    private lateinit var roomRepository: RoomHomeRepository
    private lateinit var sharedDatastore: SharedDatastore
    private lateinit var repository: ProductRepositoryImpl

    private val comte = product(id = "1", name = "Comté", type = "CHEESE")
    private val morbier = product(id = "2", name = "Morbier", type = "CHEESE")
    private val lait = product(id = "3", name = "Lait cru", type = "MILK")

    private fun product(
        id: String,
        name: String,
        type: String = "CHEESE",
        isAvailable: Boolean = true
    ) = Product(
        id = id,
        name = name,
        priceInCents = 1000L,
        imageUrl = "",
        type = type,
        isAvailable = isAvailable
    )

    @Before
    fun setUp() {
        firebaseRepository = mockk(relaxed = true)
        roomRepository = mockk(relaxed = true)
        sharedDatastore = mockk(relaxed = true)
        repository = ProductRepositoryImpl(firebaseRepository, roomRepository, sharedDatastore)
    }

    @Test
    fun `returns local products when cache is valid and not empty`() = runTest {
        every { sharedDatastore.shouldRefreshProducts } returns flowOf(false)
        coEvery { roomRepository.getProducts() } returns listOf(morbier, comte)

        val result = repository.getAllProducts(forceRefresh = false)

        assertTrue(result is DataResult.Success)
        assertEquals(listOf(comte, morbier), (result as DataResult.Success).data)
        coVerify(exactly = 0) { firebaseRepository.getAllProducts() }
    }

    @Test
    fun `syncs from remote when refresh flag is set`() = runTest {
        every { sharedDatastore.shouldRefreshProducts } returns flowOf(true)
        coEvery { firebaseRepository.getAllProducts() } returns DataResult.Success(listOf(comte))
        coEvery { roomRepository.getProducts() } returns listOf(comte)

        val result = repository.getAllProducts(forceRefresh = false)

        assertTrue(result is DataResult.Success)
        coVerify(exactly = 1) { firebaseRepository.getAllProducts() }
        coVerify(exactly = 1) { roomRepository.persistProduct(comte) }
        coVerify(exactly = 1) { sharedDatastore.setShouldRefreshProducts(false) }
    }

    @Test
    fun `syncs from remote when local cache is empty`() = runTest {
        every { sharedDatastore.shouldRefreshProducts } returns flowOf(false)
        coEvery { roomRepository.getProducts() } returns emptyList()
        coEvery { firebaseRepository.getAllProducts() } returns DataResult.Success(listOf(comte))

        val result = repository.getAllProducts(forceRefresh = false)

        assertTrue(result is DataResult.Success)
        coVerify(exactly = 1) { firebaseRepository.getAllProducts() }
    }

    @Test
    fun `forceRefresh bypasses cache even when flag is false`() = runTest {
        every { sharedDatastore.shouldRefreshProducts } returns flowOf(false)
        coEvery { firebaseRepository.getAllProducts() } returns DataResult.Success(listOf(comte))
        coEvery { roomRepository.getProducts() } returns listOf(comte)

        repository.getAllProducts(forceRefresh = true)

        coVerify(exactly = 1) { firebaseRepository.getAllProducts() }
    }

    @Test
    fun `sync deletes local products that disappeared from remote`() = runTest {
        every { sharedDatastore.shouldRefreshProducts } returns flowOf(true)
        coEvery { firebaseRepository.getAllProducts() } returns DataResult.Success(listOf(comte))
        coEvery { roomRepository.getProducts() } returns listOf(comte, morbier)

        repository.getAllProducts(forceRefresh = false)

        coVerify(exactly = 1) { roomRepository.deleteProduct(morbier) }
        coVerify(exactly = 0) { roomRepository.deleteProduct(comte) }
    }

    @Test
    fun `sync propagates remote error without touching refresh flag`() = runTest {
        every { sharedDatastore.shouldRefreshProducts } returns flowOf(true)
        val error = DataResult.Error(message = "boom")
        coEvery { firebaseRepository.getAllProducts() } returns error

        val result = repository.getAllProducts(forceRefresh = false)

        assertEquals(error, result)
        coVerify(exactly = 0) { sharedDatastore.setShouldRefreshProducts(any()) }
    }

    @Test
    fun `products are sorted by availability first then by name`() = runTest {
        val unavailable = product(id = "0", name = "Aaa épuisé", isAvailable = false)
        every { sharedDatastore.shouldRefreshProducts } returns flowOf(false)
        coEvery { roomRepository.getProducts() } returns listOf(unavailable, morbier, comte)

        val result = repository.getAllProducts(forceRefresh = false) as DataResult.Success

        assertEquals(listOf(comte, morbier, unavailable), result.data)
    }

    @Test
    fun `getAllCheeses filters non cheese products`() = runTest {
        every { sharedDatastore.shouldRefreshProducts } returns flowOf(false)
        coEvery { roomRepository.getProducts() } returns listOf(comte, lait, morbier)

        val result = repository.getAllCheeses(forceRefresh = false) as DataResult.Success

        assertEquals(listOf(comte, morbier), result.data)
    }

    @Test
    fun `getAllCheeses propagates errors`() = runTest {
        every { sharedDatastore.shouldRefreshProducts } returns flowOf(true)
        val error = DataResult.Error(message = "boom")
        coEvery { firebaseRepository.getAllProducts() } returns error

        assertEquals(error, repository.getAllCheeses(forceRefresh = false))
    }
}
