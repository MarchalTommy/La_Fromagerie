package com.mtdevelopment.home.domain.usecase

import com.mtdevelopment.core.repository.SharedDatastore
import com.mtdevelopment.core.model.Product
import com.mtdevelopment.home.domain.repository.FirebaseHomeRepository
import com.mtdevelopment.home.domain.repository.RoomRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GetAllProductsUseCase(
    private val firebaseHomeRepository: FirebaseHomeRepository,
    private val roomRepository: RoomRepository,
    private val sharedDatastore: SharedDatastore
) {

    suspend operator fun invoke(
        scope: CoroutineScope,
        onSuccess: (List<Product>) -> Unit,
        onFailure: () -> Unit
    ) {

        val lastFirestoreUpdateTimestamp = sharedDatastore.lastFirestoreUpdateTimeStamp.first()

        firebaseHomeRepository.getLastDatabaseUpdate(onSuccess = {
            /**
             * Update last firestore database change in local datastore to compare later on
             */
            scope.launch {
                sharedDatastore.lastFirestoreUpdateTimestamp(it)
            }

            /**
             * If locally known last firestore update timestamp is different from the one on the server, we need to update.
             * Else, fetch from room.
             */
            if (lastFirestoreUpdateTimestamp != it || it == 0L) {
                firebaseHomeRepository.getAllProducts(onSuccess = { productsList ->
                    /**
                     * Persist all products from firebase to room
                     */
                    productsList.forEach { product ->
                        scope.launch {
                            roomRepository.persistProduct(product)
                        }
                    }

                    /**
                     * Delete all products from room that are not in firebase,
                     * in case products got removed
                     */
                    scope.launch {
                        roomRepository.getProducts { localProductsList ->
                            localProductsList.forEach { entity ->
                                if (!productsList.any { data -> entity.id == data.id }) {
                                    scope.launch {
                                        roomRepository.deleteProduct(entity)
                                    }
                                }
                            }
                        }
                    }

                    onSuccess(productsList)
                }, onFailure)
            } else {
                scope.launch {
                    roomRepository.getProducts { localProductsList ->
                        onSuccess.invoke(localProductsList)
                    }
                }
            }

        }, onFailure = {
            scope.launch {
                roomRepository.getProducts { localProductsList ->
                    onSuccess.invoke(localProductsList)
                }
            }
        })
    }
}