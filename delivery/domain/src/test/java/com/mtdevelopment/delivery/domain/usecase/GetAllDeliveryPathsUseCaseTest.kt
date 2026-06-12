package com.mtdevelopment.delivery.domain.usecase

import com.mtdevelopment.core.repository.SharedDatastore
import com.mtdevelopment.delivery.domain.model.DeliveryPath
import com.mtdevelopment.delivery.domain.repository.FirestorePathRepository
import com.mtdevelopment.delivery.domain.repository.RoomDeliveryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetAllDeliveryPathsUseCaseTest {

    private lateinit var roomRepository: RoomDeliveryRepository
    private lateinit var sharedDatastore: SharedDatastore
    private lateinit var firestoreRepository: FirestorePathRepository
    private lateinit var useCase: GetAllDeliveryPathsUseCase

    private fun path(id: String, name: String) = DeliveryPath(
        id = id,
        pathName = name,
        availableCities = listOf("Pontarlier" to 25300),
        locations = listOf(46.9 to 6.35),
        deliveryDay = "Lundi",
        geoJson = null
    )

    private val remotePath = path("1", "Tournée du Lundi")
    private val stalePath = path("2", "Ancienne tournée")

    @Before
    fun setUp() {
        roomRepository = mockk(relaxed = true)
        sharedDatastore = mockk(relaxed = true)
        firestoreRepository = mockk()
        useCase = GetAllDeliveryPathsUseCase(roomRepository, sharedDatastore, firestoreRepository)
    }

    @Test
    fun `loads from local cache when no refresh is needed`() = runTest {
        every { sharedDatastore.shouldRefreshPaths } returns flowOf(false)
        coEvery { roomRepository.getPaths(any()) } answers {
            firstArg<(List<DeliveryPath>) -> Unit>().invoke(listOf(remotePath))
        }

        var received: List<DeliveryPath?>? = null
        useCase(
            scope = this,
            onSuccess = { received = it },
            onFailure = { }
        )
        advanceUntilIdle()

        assertEquals(listOf(remotePath), received)
        coVerify(exactly = 0) { firestoreRepository.getAllDeliveryPaths(any(), any(), any()) }
    }

    @Test
    fun `syncs from firestore when refresh flag is set`() = runTest {
        every { sharedDatastore.shouldRefreshPaths } returns flowOf(true)
        every {
            firestoreRepository.getAllDeliveryPaths(any(), any(), any())
        } answers {
            secondArg<(List<DeliveryPath?>) -> Unit>().invoke(listOf(remotePath))
        }
        coEvery { roomRepository.getPaths(any()) } answers {
            firstArg<(List<DeliveryPath>) -> Unit>().invoke(listOf(remotePath, stalePath))
        }

        var received: List<DeliveryPath?>? = null
        useCase(
            scope = this,
            onSuccess = { received = it },
            onFailure = { }
        )
        advanceUntilIdle()

        assertEquals(listOf(remotePath), received)
        coVerify(exactly = 1) { roomRepository.persistPath(remotePath) }
        // Cleanup: the stale path is removed from the local cache
        coVerify(exactly = 1) { roomRepository.deletePath(stalePath) }
        coVerify(exactly = 0) { roomRepository.deletePath(remotePath) }
        coVerify(exactly = 1) { sharedDatastore.setShouldRefreshPaths(false) }
    }

    @Test
    fun `forceRefresh bypasses the local flag`() = runTest {
        every { sharedDatastore.shouldRefreshPaths } returns flowOf(false)
        every {
            firestoreRepository.getAllDeliveryPaths(any(), any(), any())
        } answers {
            secondArg<(List<DeliveryPath?>) -> Unit>().invoke(listOf(remotePath))
        }
        coEvery { roomRepository.getPaths(any()) } answers {
            firstArg<(List<DeliveryPath>) -> Unit>().invoke(listOf(remotePath))
        }

        var received: List<DeliveryPath?>? = null
        useCase(
            forceRefresh = true,
            scope = this,
            onSuccess = { received = it },
            onFailure = { }
        )
        advanceUntilIdle()

        assertEquals(listOf(remotePath), received)
    }

    @Test
    fun `keeps refresh flag set and reports failure on network error`() = runTest {
        every { sharedDatastore.shouldRefreshPaths } returns flowOf(true)
        every {
            firestoreRepository.getAllDeliveryPaths(any(), any(), any())
        } answers {
            thirdArg<() -> Unit>().invoke()
        }

        var success = false
        var failure = false
        useCase(
            scope = this,
            onSuccess = { success = true },
            onFailure = { failure = true }
        )
        advanceUntilIdle()

        assertFalse(success)
        assertTrue(failure)
        coVerify(exactly = 1) { sharedDatastore.setShouldRefreshPaths(true) }
    }
}
