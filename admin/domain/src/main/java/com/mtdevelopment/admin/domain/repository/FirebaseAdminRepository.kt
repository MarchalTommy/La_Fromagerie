package com.mtdevelopment.admin.domain.repository

import com.mtdevelopment.core.model.DeliveryPath
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.PreparationStatus
import com.mtdevelopment.core.model.Product

/**
 * Repository interface for administrative actions performed via Firebase.
 * This repository handles management of products, delivery paths, orders, and preparation statuses.
 */
interface FirebaseAdminRepository {

    /**
     * Updates the timestamp for the last product database update.
     * @param timestamp The new timestamp to save.
     * @return Result indicating success or failure.
     */
    suspend fun saveNewDatabaseProductsUpdate(timestamp: Long): Result<Unit>

    /**
     * Adds a new product to the database.
     * @param product The product to add.
     * @return Result indicating success or failure.
     */
    suspend fun addNewProduct(product: Product): Result<Unit>

    /**
     * Updates an existing product in the database.
     * @param product The product with updated information.
     * @return Result indicating success or failure.
     */
    suspend fun updateProduct(product: Product): Result<Unit>

    /**
     * Deletes a product from the database.
     * @param product The product to delete.
     * @return Result indicating success or failure.
     */
    suspend fun deleteProduct(product: Product): Result<Unit>

    /**
     * Updates the timestamp for the last delivery path database update.
     * @param timestamp The new timestamp to save.
     * @return Result indicating success or failure.
     */
    suspend fun saveNewDatabasePathsUpdate(timestamp: Long): Result<Unit>

    /**
     * Adds a new delivery path to the database.
     * @param path The delivery path to add.
     * @return Result indicating success or failure.
     */
    suspend fun addNewDeliveryPath(path: DeliveryPath): Result<Unit>

    /**
     * Updates an existing delivery path in the database.
     * @param path The delivery path with updated information.
     * @return Result indicating success or failure.
     */
    suspend fun updateDeliveryPath(path: DeliveryPath): Result<Unit>

    /**
     * Deletes a delivery path from the database.
     * @param path The delivery path to delete.
     * @return Result indicating success or failure.
     */
    suspend fun deleteDeliveryPath(path: DeliveryPath): Result<Unit>

    /**
     * Retrieves all orders from the database.
     * @param onSuccess Callback invoked with the list of orders, or null if an error occurs.
     */
    suspend fun getAllOrders(onSuccess: (List<Order>?) -> Unit)

    /**
     * Retrieves all possible preparation statuses from the database.
     * @param onSuccess Callback invoked with the list of preparation statuses, or null if an error occurs.
     */
    suspend fun getPreparationStatuses(onSuccess: (List<PreparationStatus>?) -> Unit)

    /**
     * Updates an existing preparation status.
     * @param status The updated preparation status.
     * @return Result indicating success or failure.
     */
    suspend fun updatePreparationStatus(status: PreparationStatus): Result<Unit>

}