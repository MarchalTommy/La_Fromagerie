package com.mtdevelopment.home.domain.usecase

import com.mtdevelopment.core.repository.SharedDatastore
import com.mtdevelopment.home.domain.repository.FirebaseHomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class GetLastFirestoreDatabaseUpdateUseCase(
    private val firebaseHomeRepository: FirebaseHomeRepository,
    private val sharedDatastore: SharedDatastore
) {
    suspend operator fun invoke(
        onSuccess: () -> Unit, onFailure: () -> Unit
    ) {
        val shouldRefreshProducts: MutableStateFlow<Boolean> = MutableStateFlow(false)
        val shouldRefreshPaths: MutableStateFlow<Boolean> = MutableStateFlow(false)

        val newProductUpdateTimestamp: MutableStateFlow<Long> = MutableStateFlow(0L)
        val newPathUpdateTimestamp: MutableStateFlow<Long> = MutableStateFlow(0L)

        // GET CURRENT SAVED TIMESTAMPS
        val lastProductUpdate = sharedDatastore.lastFirestoreProductsUpdate
        val lastPathUpdate = sharedDatastore.lastFirestorePathsUpdate

        firebaseHomeRepository.getLastDatabaseUpdate(
            onSuccess = { productsTimestamp, pathsTimestamp ->
                newProductUpdateTimestamp.tryEmit(productsTimestamp)
                newPathUpdateTimestamp.tryEmit(pathsTimestamp)
            }, onFailure
        )

        lastProductUpdate.combine(lastPathUpdate) { product, path ->
            Pair(product, path)
        }.collect { pair ->
            val product = pair.first
            val path = pair.second

            // IF NOT SAME AS PREVIOUS ONES, SET REFRESH FLAG
            if (product != newProductUpdateTimestamp.value || product == 0L) {
                shouldRefreshProducts.tryEmit(true)
            }

            if (path != newPathUpdateTimestamp.value || path == 0L) {
                shouldRefreshPaths.tryEmit(true)
            }

            // UPDATE REFRESH FLAGS
            sharedDatastore.setShouldRefreshProducts(shouldRefreshProducts.value)
            sharedDatastore.setShouldRefreshPaths(shouldRefreshProducts.value)

            // UPDATE SAVED TIMESTAMPS
            sharedDatastore.lastFirestoreProductsUpdate(newProductUpdateTimestamp.value)
            sharedDatastore.lastFirestorePathsUpdate(newPathUpdateTimestamp.value)

            onSuccess.invoke()
        }

    }
}