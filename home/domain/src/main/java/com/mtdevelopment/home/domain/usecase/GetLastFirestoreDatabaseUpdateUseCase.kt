package com.mtdevelopment.home.domain.usecase

import android.content.ContentValues.TAG
import android.util.Log
import com.mtdevelopment.core.repository.SharedDatastore
import com.mtdevelopment.home.domain.repository.FirebaseHomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

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

        newProductUpdateTimestamp.combine(newPathUpdateTimestamp) { newProductTimestamp, newPathTimestamp ->
            Pair(newProductTimestamp, newPathTimestamp)
        }.collect { pair ->
            val productTimestamp = pair.first
            val pathTimestamp = pair.second

            if (newProductUpdateTimestamp.value != 0L && newPathUpdateTimestamp.value != 0L) {
                // IF NOT SAME AS PREVIOUS ONES, SET REFRESH FLAG
                if (lastProductUpdate.first() != productTimestamp || lastProductUpdate.first() == 0L) {
                    shouldRefreshProducts.tryEmit(true)
                }

                if (lastPathUpdate.first() != pathTimestamp || lastPathUpdate.first() == 0L) {
                    shouldRefreshPaths.tryEmit(true)
                }

                // UPDATE REFRESH FLAGS
                sharedDatastore.setShouldRefreshProducts(shouldRefreshProducts.value)
                sharedDatastore.setShouldRefreshPaths(shouldRefreshProducts.value)

                // UPDATE SAVED TIMESTAMPS
                sharedDatastore.lastFirestoreProductsUpdate(newProductUpdateTimestamp.value)
                sharedDatastore.lastFirestorePathsUpdate(newPathUpdateTimestamp.value)

                Log.i(
                    TAG,
                    "SHOULD UPDATE PRODUCT : ${shouldRefreshProducts.value}\nSHOULD UPDATE PATH : ${shouldRefreshPaths.value}"
                )
                onSuccess.invoke()
            }
        }
    }
}