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
     * @param onSuccess Callback invoked with the [Product] if found.
     */
    suspend fun getProductById(id: String, onSuccess: (Product) -> Unit)

    /**
     * Retrieves all cheeses from the local database.
     * @param onSuccess Callback invoked with the list of cheese [Product] objects.
     */
    suspend fun getCheeses(onSuccess: (List<Product>) -> Unit)

    /**
     * Retrieves all products from the local database.
     * @param onSuccess Callback invoked with the full list of [Product] objects.
     */
    suspend fun getProducts(onSuccess: (List<Product>) -> Unit)
}