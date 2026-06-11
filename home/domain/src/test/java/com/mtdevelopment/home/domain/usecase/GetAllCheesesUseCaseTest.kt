package com.mtdevelopment.home.domain.usecase

import com.mtdevelopment.core.model.Product
import com.mtdevelopment.home.domain.repository.RoomHomeRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetAllCheesesUseCaseTest {

    @Test
    fun `invoke forwards cheeses from local cache to callback`() = runTest {
        val cheeses = listOf(
            Product(
                id = "1",
                name = "Comté",
                priceInCents = 1200L,
                imageUrl = "",
                type = "CHEESE"
            )
        )
        val roomHomeRepository = mockk<RoomHomeRepository> {
            coEvery { getCheeses() } returns cheeses
        }

        var received: List<Product>? = null
        GetAllCheesesUseCase(roomHomeRepository).invoke { received = it }

        assertEquals(cheeses, received)
    }

    @Test
    fun `invoke forwards empty list when cache has no cheese`() = runTest {
        val roomHomeRepository = mockk<RoomHomeRepository> {
            coEvery { getCheeses() } returns emptyList()
        }

        var received: List<Product>? = null
        GetAllCheesesUseCase(roomHomeRepository).invoke { received = it }

        assertEquals(emptyList<Product>(), received)
    }
}
