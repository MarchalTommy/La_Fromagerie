package com.mtdevelopment.home.domain.usecase

import android.content.ContentValues.TAG
import android.util.Log
import com.mtdevelopment.core.repository.SharedDatastore
import com.mtdevelopment.core.util.DataResult
import com.mtdevelopment.home.domain.repository.FirebaseHomeRepository
import kotlinx.coroutines.flow.first

/**
 * Use case to check for global database updates on the server.
 * It compares the remote update timestamps for products and delivery paths with the locally stored ones.
 * If a mismatch is detected, it flags the local cache as needing a refresh.
 */
class GetLastFirestoreDatabaseUpdateUseCase(
    private val firebaseHomeRepository: FirebaseHomeRepository,
    private val sharedDatastore: SharedDatastore
) {
    /**
     * Executes the use case.
     * Logic:
     * 1. Fetches current "last update" timestamps from [SharedDatastore].
     * 2. Fetches latest "last update" timestamps from [FirebaseHomeRepository].
     * 3. Compares both. If the remote timestamp is newer or different, it sets a refresh flag in [SharedDatastore].
     * 4. Updates the local timestamps in [SharedDatastore] to match the server.
     */
    suspend operator fun invoke(
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        // Fetch latest timestamps from server
        val result = firebaseHomeRepository.getLastDatabaseUpdate()

        if (result is DataResult.Success) {
            val (productTimestamp, pathTimestamp) = result.data

            // Retrieve current saved timestamps
            val lastProductUpdateVal = sharedDatastore.lastFirestoreProductsUpdate.first()
            val lastPathUpdateVal = sharedDatastore.lastFirestorePathsUpdate.first()

            val shouldRefreshProducts =
                lastProductUpdateVal != productTimestamp || lastProductUpdateVal == 0L
            val shouldRefreshPaths = lastPathUpdateVal != pathTimestamp || lastPathUpdateVal == 0L

            // Update refresh flags in Datastore
            sharedDatastore.setShouldRefreshProducts(shouldRefreshProducts)
            sharedDatastore.setShouldRefreshPaths(shouldRefreshPaths)

            // Update saved timestamps in Datastore
            sharedDatastore.lastFirestoreProductsUpdate(productTimestamp)
            sharedDatastore.lastFirestorePathsUpdate(pathTimestamp)

            Log.i(
                TAG,
                "SHOULD UPDATE PRODUCT : $shouldRefreshProducts\nSHOULD UPDATE PATH : $shouldRefreshPaths"
            )
            onSuccess()
        } else {
            onFailure()
        }
    }
}