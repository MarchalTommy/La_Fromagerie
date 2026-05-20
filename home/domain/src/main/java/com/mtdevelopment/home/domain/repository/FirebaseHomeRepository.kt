package com.mtdevelopment.home.domain.repository

import com.mtdevelopment.core.model.Product
import com.mtdevelopment.core.util.DataResult

/**
 * Repository interface for fetching product and database update information from Firebase.
 */
interface FirebaseHomeRepository {

    /**
     * Retrieves all products available in the database.
     */
    suspend fun getAllProducts(): DataResult<List<Product>>

    /**
     * Retrieves all products categorized as cheeses.
     */
    suspend fun getAllCheeses(): DataResult<List<Product>>

    /**
     * Retrieves the timestamps of the last database updates for products and delivery paths.
     * This is used to determine if the local cache needs synchronization.
     * @return Pair of product and path update timestamps.
     */
    suspend fun getLastDatabaseUpdate(): DataResult<Pair<Long, Long>>

}