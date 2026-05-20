package com.mtdevelopment.home.domain.repository

import com.mtdevelopment.core.model.Product
import com.mtdevelopment.core.util.DataResult

/**
 * Main repository for products, coordinating between remote and local data sources.
 */
interface ProductRepository {
    /**
     * Retrieves all products, synchronizing with the remote source if necessary.
     * @param forceRefresh If true, forces a fetch from the remote source.
     */
    suspend fun getAllProducts(forceRefresh: Boolean = false): DataResult<List<Product>>

    /**
     * Retrieves all cheeses, synchronizing with the remote source if necessary.
     */
    suspend fun getAllCheeses(forceRefresh: Boolean = false): DataResult<List<Product>>
}
