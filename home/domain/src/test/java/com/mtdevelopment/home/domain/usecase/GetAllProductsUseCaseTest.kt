package com.mtdevelopment.home.domain.usecase

import com.mtdevelopment.core.model.Product
import com.mtdevelopment.core.util.DataResult
import com.mtdevelopment.home.domain.repository.ProductRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAllProductsUseCaseTest {

    private val productRepository: ProductRepository = mockk()
    private val useCase = GetAllProductsUseCase(productRepository)

    @Test
    fun `invoke delegates to repository and returns success`() = runTest {
        // Arrange
        val productList = listOf(
            Product("2", "Roquefort", 1200L, "url", "cheese"),
            Product("1", "Comté", 1000L, "url", "cheese")
        )
        coEvery { productRepository.getAllProducts(any()) } returns DataResult.Success(productList)

        // Act
        val result = useCase.invoke(forceRefresh = true)

        // Assert
        assertTrue(result is DataResult.Success)
        val data = (result as DataResult.Success).data
        assertEquals(2, data.size)
        // If running in admin, it will be sorted alphabetically (Comté first, then Roquefort).
        // If client, it returns repository order (Roquefort first). Let's assert on content correctness.
        assertTrue(data.any { it.name == "Comté" })
        assertTrue(data.any { it.name == "Roquefort" })

        coVerify(exactly = 1) { productRepository.getAllProducts(true) }
    }

    @Test
    fun `invoke delegates to repository and returns error`() = runTest {
        // Arrange
        val errorMsg = "Network error"
        coEvery { productRepository.getAllProducts(any()) } returns DataResult.Error(message = errorMsg)

        // Act
        val result = useCase.invoke(forceRefresh = false)

        // Assert
        assertTrue(result is DataResult.Error)
        assertEquals(errorMsg, (result as DataResult.Error).message)

        coVerify(exactly = 1) { productRepository.getAllProducts(false) }
    }
}
