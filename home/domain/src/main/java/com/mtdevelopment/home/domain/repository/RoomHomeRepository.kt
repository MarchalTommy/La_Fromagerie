package com.mtdevelopment.home.domain.repository

import com.mtdevelopment.core.model.Product

/**
 * Repository interface for local product persistence using Room.
 */
interface RoomHomeRepository {
    /**
     * Persists a product to the local database.
     */
    suspend fun persistProduct(product: Product)

    /**
     * Deletes a product from the local database.
     */
    suspend fun deleteProduct(product: Product)

    /**
     * Updates an existing product in the local database.
     */
    suspend fun updateProduct(product: Product)

    /**
     * Retrieves a specific product by its ID.
     */
    suspend fun getProductById(id: String): Product?

    /**
     * Retrieves all cheeses from the local database.
     */
    suspend fun getCheeses(): List<Product>

    /**
     * Retrieves all products from the local database.
     */
    suspend fun getProducts(): List<Product>
}