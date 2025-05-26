package com.mtdevelopment.home.domain.usecase

import com.mtdevelopment.core.model.Product
import com.mtdevelopment.core.repository.SharedDatastore
import com.mtdevelopment.home.domain.repository.FirebaseHomeRepository
import com.mtdevelopment.home.domain.repository.RoomHomeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GetAllProductsUseCase(
    private val firebaseHomeRepository: FirebaseHomeRepository,
    private val roomHomeRepository: RoomHomeRepository,
    private val sharedDatastore: SharedDatastore
) {

    suspend operator fun invoke(
        scope: CoroutineScope,
        onSuccess: (List<Product>) -> Unit,
        onFailure: () -> Unit
    ) {

        val shouldRefresh = sharedDatastore.shouldRefreshProducts.first()

        /**
         * If locally known last firestore update timestamp is different from the one on the server, we need to update.
         * Else, fetch from room.
         */
        if (shouldRefresh) {
            firebaseHomeRepository.getAllProducts(onSuccess = { productsList ->
                /**
                 * Persist all products from firebase to room
                 */
                productsList.forEach { product ->
                    scope.launch {
                        roomHomeRepository.persistProduct(product)
                    }
                }

                scope.launch {
                    /**
                     * Reset refresh needed flag
                     */
                    sharedDatastore.setShouldRefreshProducts(false)

                    /**
                     * Delete all products from room that are not in firebase,
                     * in case products got removed
                     */
                    roomHomeRepository.getProducts { localProductsList ->
                        localProductsList.forEach { entity ->
                            if (!productsList.any { data -> entity.id == data.id }) {
                                scope.launch {
                                    roomHomeRepository.deleteProduct(entity)
                                }
                            }
                        }
                    }
                }

                val orderedProducts = productsList.sortedWith(
                    compareByDescending<Product> { it.isAvailable }
                        .thenBy { it.name }
                )

                onSuccess(orderedProducts)
            }, onFailure)
        } else {
            scope.launch {
                roomHomeRepository.getProducts { localProductsList ->
                    val orderedProducts = localProductsList.sortedWith(
                        compareByDescending<Product> { it.isAvailable }
                            .thenBy { it.name }
                    )
                    onSuccess.invoke(orderedProducts)
                }
            }
        }

    }
}