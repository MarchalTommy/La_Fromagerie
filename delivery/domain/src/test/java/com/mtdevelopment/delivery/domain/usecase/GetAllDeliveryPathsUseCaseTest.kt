package com.mtdevelopment.delivery.domain.usecase

import com.mtdevelopment.core.model.DeliveryCity
import com.mtdevelopment.core.repository.SharedDatastore
import com.mtdevelopment.delivery.domain.model.DeliveryPath
import com.mtdevelopment.delivery.domain.repository.FirestorePathRepository
import com.mtdevelopment.delivery.domain.repository.PathFetchOutcome
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
        cities = listOf(DeliveryCity("Pontarlier", 25300)),
        locations = listOf(46.9 to 6.35),
        deliveryDay = "Lundi",
        geoJson = null
    )

    private val remotePath = path("1", "Tournée du Lundi")
    private val stalePath = path("2", "Ancienne tournée")

    /** Convenience for the common case: everything Firestore listed came back enriched. */
    private fun complete(vararg paths: DeliveryPath) = PathFetchOutcome(
        paths = paths.toList(),
        remoteIds = paths.map { it.id }.toSet(),
        degraded = false
    )

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
        coEvery { roomRepository.getPathsOnce() } returns listOf(remotePath)

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
            secondArg<(PathFetchOutcome) -> Unit>().invoke(complete(remotePath))
        }
        coEvery { roomRepository.getPathsOnce() } returns listOf(remotePath, stalePath)

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
            secondArg<(PathFetchOutcome) -> Unit>().invoke(complete(remotePath))
        }
        coEvery { roomRepository.getPathsOnce() } returns listOf(remotePath)

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

    /**
     * Regression: an offline first launch made Firestore's `get()` complete successfully
     * with zero documents. That empty list used to be persisted as the truth, which reset
     * the refresh flag and left the app permanently pathless — every customer address was
     * then reported as "not on a delivery path".
     */
    @Test
    fun `empty fetch is reported as failure and keeps the refresh flag set`() = runTest {
        every { sharedDatastore.shouldRefreshPaths } returns flowOf(true)
        every {
            firestoreRepository.getAllDeliveryPaths(any(), any(), any())
        } answers {
            secondArg<(PathFetchOutcome) -> Unit>().invoke(complete())
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
        coVerify(exactly = 0) { sharedDatastore.setShouldRefreshPaths(false) }
    }

    @Test
    fun `empty fetch never wipes the local cache`() = runTest {
        every { sharedDatastore.shouldRefreshPaths } returns flowOf(true)
        every {
            firestoreRepository.getAllDeliveryPaths(any(), any(), any())
        } answers {
            secondArg<(PathFetchOutcome) -> Unit>().invoke(complete())
        }
        coEvery { roomRepository.getPathsOnce() } returns listOf(remotePath, stalePath)

        useCase(
            scope = this,
            onSuccess = { },
            onFailure = { }
        )
        advanceUntilIdle()

        coVerify(exactly = 0) { roomRepository.deletePath(any()) }
        coVerify(exactly = 0) { roomRepository.persistPath(any()) }
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

    ///////////////////////////////////////////////////////////////////////////
    // Partial fetch — the "the Friday card disappears sometimes" regression
    ///////////////////////////////////////////////////////////////////////////

    /**
     * The whole point of [PathFetchOutcome.remoteIds]. Firestore still lists both tournées, but one
     * of them — the 20-city one, whose enrichment fails whenever any single geocoding call does —
     * did not come back. Deleting it here is what used to make the admin card vanish, and the flag
     * being cleared in the same breath is what kept it gone until `path_timestamp` moved.
     */
    @Test
    fun `path still listed by firestore is never deleted just because enrichment failed`() =
        runTest {
            every { sharedDatastore.shouldRefreshPaths } returns flowOf(true)
            every {
                firestoreRepository.getAllDeliveryPaths(any(), any(), any())
            } answers {
                secondArg<(PathFetchOutcome) -> Unit>().invoke(
                    PathFetchOutcome(
                        paths = listOf(remotePath),
                        remoteIds = setOf("1", "2"),
                        degraded = true
                    )
                )
            }
            coEvery { roomRepository.getPathsOnce() } returns listOf(remotePath, stalePath)

            useCase(
                scope = this,
                onSuccess = { },
                onFailure = { }
            )
            advanceUntilIdle()

            coVerify(exactly = 0) { roomRepository.deletePath(any()) }
        }

    @Test
    fun `degraded fetch keeps the refresh flag set so the next load retries`() = runTest {
        every { sharedDatastore.shouldRefreshPaths } returns flowOf(true)
        every {
            firestoreRepository.getAllDeliveryPaths(any(), any(), any())
        } answers {
            secondArg<(PathFetchOutcome) -> Unit>().invoke(
                PathFetchOutcome(
                    paths = listOf(remotePath),
                    remoteIds = setOf("1", "2"),
                    degraded = true
                )
            )
        }
        coEvery { roomRepository.getPathsOnce() } returns listOf(remotePath, stalePath)

        useCase(
            scope = this,
            onSuccess = { },
            onFailure = { }
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { sharedDatastore.setShouldRefreshPaths(true) }
        coVerify(exactly = 0) { sharedDatastore.setShouldRefreshPaths(false) }
    }

    /**
     * The visible half of the fix: keeping the row in Room is not enough, the UI must still be
     * handed the path this session or the card disappears anyway for as long as the app is open.
     */
    @Test
    fun `path that failed enrichment is still emitted, served from cache`() = runTest {
        every { sharedDatastore.shouldRefreshPaths } returns flowOf(true)
        every {
            firestoreRepository.getAllDeliveryPaths(any(), any(), any())
        } answers {
            secondArg<(PathFetchOutcome) -> Unit>().invoke(
                PathFetchOutcome(
                    paths = listOf(remotePath),
                    remoteIds = setOf("1", "2"),
                    degraded = true
                )
            )
        }
        coEvery { roomRepository.getPathsOnce() } returns listOf(remotePath, stalePath)

        var received: List<DeliveryPath?>? = null
        useCase(
            scope = this,
            onSuccess = { received = it },
            onFailure = { }
        )
        advanceUntilIdle()

        assertEquals(listOf(remotePath, stalePath), received)
    }

    /**
     * The rescue must not resurrect a genuinely deleted tournée: a cached path absent from
     * `remoteIds` is gone, and stays gone.
     */
    @Test
    fun `cache rescue never resurrects a path firestore no longer lists`() = runTest {
        every { sharedDatastore.shouldRefreshPaths } returns flowOf(true)
        every {
            firestoreRepository.getAllDeliveryPaths(any(), any(), any())
        } answers {
            secondArg<(PathFetchOutcome) -> Unit>().invoke(complete(remotePath))
        }
        coEvery { roomRepository.getPathsOnce() } returns listOf(remotePath, stalePath)

        var received: List<DeliveryPath?>? = null
        useCase(
            scope = this,
            onSuccess = { received = it },
            onFailure = { }
        )
        advanceUntilIdle()

        assertEquals(listOf(remotePath), received)
        coVerify(exactly = 1) { roomRepository.deletePath(stalePath) }
    }
}
