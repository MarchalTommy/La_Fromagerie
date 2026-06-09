package com.mtdevelopment.home.domain.usecase

import com.mtdevelopment.core.model.Product
import com.mtdevelopment.core.util.DataResult
import com.mtdevelopment.home.domain.repository.ProductRepository

/**
 * Use case to retrieve all products for the administrator view.
 * Now delegates to [ProductRepository] and ensures alphabetical sorting for the admin dashboard.
 */
class GetAllProductsUseCase(
    private val productRepository: ProductRepository
) {

    /**
     * Executes the use case.
     * @param forceRefresh If true, forces a fetch from the remote source.
     */
    suspend operator fun invoke(forceRefresh: Boolean = false): DataResult<List<Product>> {
        val result = productRepository.getAllProducts(forceRefresh)

        return if (result is DataResult.Success) {
            // Admin sorting: purely alphabetical by name
            DataResult.Success(result.data.sortedBy { it.name })
        } else {
            result
        }
    }
}