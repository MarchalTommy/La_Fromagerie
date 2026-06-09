package com.mtdevelopment.home.data.repository

import com.mtdevelopment.core.model.Product
import com.mtdevelopment.core.repository.SharedDatastore
import com.mtdevelopment.core.util.DataResult
import com.mtdevelopment.home.domain.repository.FirebaseHomeRepository
import com.mtdevelopment.home.domain.repository.ProductRepository
import com.mtdevelopment.home.domain.repository.RoomHomeRepository
import kotlinx.coroutines.flow.first

class ProductRepositoryImpl(
    private val firebaseRepository: FirebaseHomeRepository,
    private val roomRepository: RoomHomeRepository,
    private val sharedDatastore: SharedDatastore
) : ProductRepository {

    override suspend fun getAllProducts(forceRefresh: Boolean): DataResult<List<Product>> {
        val shouldRefresh = forceRefresh || sharedDatastore.shouldRefreshProducts.first()

        return if (shouldRefresh) {
            syncProducts()
        } else {
            val localProducts = roomRepository.getProducts()
            if (localProducts.isEmpty()) {
                syncProducts()
            } else {
                DataResult.Success(localProducts.sortedWith(productComparator))
            }
        }
    }

    override suspend fun getAllCheeses(forceRefresh: Boolean): DataResult<List<Product>> {
        // For simplicity, we sync everything and then filter, or just use the local cache if available
        val allProductsResult = getAllProducts(forceRefresh)
        return if (allProductsResult is DataResult.Success) {
            DataResult.Success(allProductsResult.data.filter { it.type.lowercase() == "cheese" })
        } else {
            allProductsResult
        }
    }

    private suspend fun syncProducts(): DataResult<List<Product>> {
        return when (val remoteResult = firebaseRepository.getAllProducts()) {
            is DataResult.Success -> {
                val remoteProducts = remoteResult.data

                // Update local cache
                remoteProducts.forEach { roomRepository.persistProduct(it) }

                // Cleanup local cache
                val localProducts = roomRepository.getProducts()
                localProducts.forEach { local ->
                    if (remoteProducts.none { it.id == local.id }) {
                        roomRepository.deleteProduct(local)
                    }
                }

                sharedDatastore.setShouldRefreshProducts(false)
                DataResult.Success(remoteProducts.sortedWith(productComparator))
            }

            is DataResult.Error -> remoteResult
            is DataResult.Loading -> DataResult.Loading
        }
    }

    private val productComparator = compareByDescending<Product> { it.isAvailable }
        .thenBy { it.name }
}
