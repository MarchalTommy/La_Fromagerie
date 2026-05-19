package com.mtdevelopment.home.domain.repository

import com.mtdevelopment.core.model.Product

/**
 * Repository interface for fetching product and database update information from Firebase.
 */
interface FirebaseHomeRepository {

    /**
     * Retrieves all products available in the database.
     * @param onSuccess Callback invoked with the list of [Product] on success.
     * @param onFailure Callback invoked when an error occurs.
     */
    fun getAllProducts(onSuccess: (List<Product>) -> Unit, onFailure: () -> Unit)

    /**
     * Retrieves all products categorized as cheeses.
     * @param onSuccess Callback invoked with the list of [Product] on success.
     * @param onFailure Callback invoked when an error occurs.
     */
    fun getAllCheeses(onSuccess: (List<Product>) -> Unit, onFailure: () -> Unit)

    /**
     * Retrieves the timestamps of the last database updates for products and delivery paths.
     * This is used to determine if the local cache needs synchronization.
     * @param onSuccess Callback invoked with product and path update timestamps.
     * @param onFailure Callback invoked when an error occurs.
     */
    fun getLastDatabaseUpdate(onSuccess: (products: Long, paths: Long) -> Unit, onFailure: () -> Unit)

}