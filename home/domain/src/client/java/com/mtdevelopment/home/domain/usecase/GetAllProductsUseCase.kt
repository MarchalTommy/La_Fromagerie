package com.mtdevelopment.home.domain.usecase

import com.mtdevelopment.core.model.Product
import com.mtdevelopment.core.util.DataResult
import com.mtdevelopment.home.domain.repository.ProductRepository

/**
 * Use case to retrieve all products for the client view.
 * Now delegates to [ProductRepository] for synchronization and caching logic.
 */
class GetAllProductsUseCase(
    private val productRepository: ProductRepository
) {

    /**
     * Executes the use case.
     * @param forceRefresh If true, forces a fetch from the remote source.
     */
    suspend operator fun invoke(forceRefresh: Boolean = false): DataResult<List<Product>> {
        return productRepository.getAllProducts(forceRefresh)
    }
}