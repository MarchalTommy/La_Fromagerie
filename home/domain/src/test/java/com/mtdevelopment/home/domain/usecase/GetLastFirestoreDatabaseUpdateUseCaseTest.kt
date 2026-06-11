package com.mtdevelopment.home.domain.usecase

import android.util.Log
import com.mtdevelopment.core.repository.SharedDatastore
import com.mtdevelopment.core.util.DataResult
import com.mtdevelopment.home.domain.repository.FirebaseHomeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetLastFirestoreDatabaseUpdateUseCaseTest {

    private lateinit var firebaseHomeRepository: FirebaseHomeRepository
    private lateinit var sharedDatastore: SharedDatastore
    private lateinit var useCase: GetLastFirestoreDatabaseUpdateUseCase

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.i(any(), any<String>()) } returns 0

        firebaseHomeRepository = mockk()
        sharedDatastore = mockk(relaxed = true)
        useCase = GetLastFirestoreDatabaseUpdateUseCase(firebaseHomeRepository, sharedDatastore)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `sets refresh flags when remote timestamps differ from local`() = runTest {
        coEvery { firebaseHomeRepository.getLastDatabaseUpdate() } returns
                DataResult.Success(100L to 200L)
        every { sharedDatastore.lastFirestoreProductsUpdate } returns flowOf(50L)
        every { sharedDatastore.lastFirestorePathsUpdate } returns flowOf(200L)

        var success = false
        useCase(onSuccess = { success = true }, onFailure = {})

        assertTrue(success)
        coVerify { sharedDatastore.setShouldRefreshProducts(true) }
        coVerify { sharedDatastore.setShouldRefreshPaths(false) }
        coVerify { sharedDatastore.lastFirestoreProductsUpdate(100L) }
        coVerify { sharedDatastore.lastFirestorePathsUpdate(200L) }
    }

    @Test
    fun `forces refresh when local timestamps were never initialized`() = runTest {
        coEvery { firebaseHomeRepository.getLastDatabaseUpdate() } returns
                DataResult.Success(0L to 0L)
        every { sharedDatastore.lastFirestoreProductsUpdate } returns flowOf(0L)
        every { sharedDatastore.lastFirestorePathsUpdate } returns flowOf(0L)

        useCase(onSuccess = {}, onFailure = {})

        coVerify { sharedDatastore.setShouldRefreshProducts(true) }
        coVerify { sharedDatastore.setShouldRefreshPaths(true) }
    }

    @Test
    fun `does not flag refresh when timestamps match`() = runTest {
        coEvery { firebaseHomeRepository.getLastDatabaseUpdate() } returns
                DataResult.Success(100L to 200L)
        every { sharedDatastore.lastFirestoreProductsUpdate } returns flowOf(100L)
        every { sharedDatastore.lastFirestorePathsUpdate } returns flowOf(200L)

        useCase(onSuccess = {}, onFailure = {})

        coVerify { sharedDatastore.setShouldRefreshProducts(false) }
        coVerify { sharedDatastore.setShouldRefreshPaths(false) }
    }

    @Test
    fun `invokes onFailure and leaves datastore untouched on error`() = runTest {
        coEvery { firebaseHomeRepository.getLastDatabaseUpdate() } returns
                DataResult.Error(message = "boom")

        var success = false
        var failure = false
        useCase(onSuccess = { success = true }, onFailure = { failure = true })

        assertFalse(success)
        assertTrue(failure)
        coVerify(exactly = 0) { sharedDatastore.setShouldRefreshProducts(any()) }
        coVerify(exactly = 0) { sharedDatastore.setShouldRefreshPaths(any()) }
    }
}
