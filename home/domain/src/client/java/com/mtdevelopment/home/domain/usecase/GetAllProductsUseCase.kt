package com.mtdevelopment.home.domain.usecase

import com.mtdevelopment.core.model.Product
import com.mtdevelopment.core.repository.SharedDatastore
import com.mtdevelopment.home.domain.repository.FirebaseHomeRepository
import com.mtdevelopment.home.domain.repository.RoomHomeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Use case to retrieve all products for the client view, implementing a synchronization 
 * strategy between remote Firestore and local Room database.
 * 
 * Logic flow:
 * 1. Checks if a refresh is needed via the `shouldRefreshProducts` flag in [SharedDatastore].
 * 2. If refresh is needed:
 *    - Fetches all products from [FirebaseHomeRepository].
 *    - Persists each product into the local [RoomHomeRepository].
 *    - Performs cleanup: deletes local products no longer present on the server.
 *    - Resets the `shouldRefreshProducts` flag.
 *    - Returns the list sorted by availability and name.
 * 3. If no refresh is needed:
 *    - Fetches products from local [RoomHomeRepository] for speed and offline access.
 *    - Returns the list sorted by availability and name.
 */
class GetAllProductsUseCase(
    private val firebaseHomeRepository: FirebaseHomeRepository,
    private val roomHomeRepository: RoomHomeRepository,
    private val sharedDatastore: SharedDatastore
) {

    /**
     * Executes the use case.
     * @param scope CoroutineScope for background database operations.
     * @param onSuccess Callback invoked with the sorted list of [Product].
     * @param onFailure Callback invoked on network error.
     */
    suspend operator fun invoke(
        scope: CoroutineScope,
        onSuccess: (List<Product>) -> Unit,
        onFailure: () -> Unit
    ) {

        val shouldRefresh = sharedDatastore.shouldRefreshProducts.first()

        if (shouldRefresh) {
            firebaseHomeRepository.getAllProducts(onSuccess = { productsList ->
                /**
                 * Cache synchronization: Persist new/updated products to Room
                 */
                productsList.forEach { product ->
                    scope.launch {
                        roomHomeRepository.persistProduct(product)
                    }
                }

                scope.launch {
                    /**
                     * Mark local cache as synchronized
                     */
                    sharedDatastore.setShouldRefreshProducts(false)

                    /**
                     * Cache cleanup: Remove local products no longer on server
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

                // Sorting: Available products first, then alphabetically by name
                val orderedProducts = productsList.sortedWith(
                    compareByDescending<Product> { it.isAvailable }
                        .thenBy { it.name }
                )

                onSuccess(orderedProducts)
            }, onFailure)
        } else {
            // Fast path: Load from local database
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