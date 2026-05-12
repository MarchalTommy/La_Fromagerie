package com.mtdevelopment.admin.data.repository

import com.mtdevelopment.admin.data.model.toDataDeliveryPath
import com.mtdevelopment.admin.data.source.FirestoreAdminDatasource
import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.DeliveryPath
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.PreparationStatus
import com.mtdevelopment.core.model.toData
import com.mtdevelopment.core.model.toDomain
import com.mtdevelopment.core.model.toOrder
import com.mtdevelopment.core.model.toProductData

/**
 * Implementation of [FirebaseAdminRepository] that interacts with [FirestoreAdminDatasource].
 * This implementation ensures that every time a product or a delivery path is modified (added, updated, or deleted),
 * a global timestamp in the database is updated to signal changes to other clients.
 */
class FirebaseAdminRepositoryImpl(
    private val firestore: FirestoreAdminDatasource
) : FirebaseAdminRepository {

    ///////////////////////////////////////////////////////////////////////////
    // Product Management
    // Each modification triggers a call to saveNewDatabaseProductsUpdate.
    ///////////////////////////////////////////////////////////////////////////
    override suspend fun addNewProduct(product: com.mtdevelopment.core.model.Product): Result<Unit> {
        val result = firestore.addNewProduct(product = product.toProductData())
        var finalResult: Result<Unit>? = null

        result.onSuccess {
            // Signal database update for products
            finalResult = saveNewDatabaseProductsUpdate(System.currentTimeMillis())
        }

        return finalResult ?: result
    }

    override suspend fun updateProduct(product: com.mtdevelopment.core.model.Product): Result<Unit> {
        val result = firestore.updateProduct(product = product.toProductData())
        var finalResult: Result<Unit>? = null

        result.onSuccess {
            // Signal database update for products
            finalResult = saveNewDatabaseProductsUpdate(System.currentTimeMillis())
        }

        return finalResult ?: result
    }

    override suspend fun deleteProduct(product: com.mtdevelopment.core.model.Product): Result<Unit> {
        val result = firestore.deleteProduct(product = product.toProductData())
        var finalResult: Result<Unit>? = null

        result.onSuccess {
            // Signal database update for products
            finalResult = saveNewDatabaseProductsUpdate(System.currentTimeMillis())
        }

        return finalResult ?: result
    }

    //////////////////////////////////////////////////////////////////////////
    // Delivery Path Management
    // Each modification triggers a call to saveNewDatabasePathsUpdate.
    ///////////////////////////////////////////////////////////////////////////
    override suspend fun addNewDeliveryPath(path: DeliveryPath): Result<Unit> {
        val result = firestore.addNewDeliveryPath(path = path.toDataDeliveryPath())
        var finalResult: Result<Unit>? = null

        result.onSuccess {
            // Signal database update for delivery paths
            finalResult = saveNewDatabasePathsUpdate(System.currentTimeMillis())
        }

        return finalResult ?: result
    }

    override suspend fun updateDeliveryPath(path: DeliveryPath): Result<Unit> {
        val result = firestore.updateDeliveryPath(path = path.toDataDeliveryPath())
        var finalResult: Result<Unit>? = null

        result.onSuccess {
            // Signal database update for delivery paths
            finalResult = saveNewDatabasePathsUpdate(System.currentTimeMillis())
        }

        return finalResult ?: result
    }

    override suspend fun deleteDeliveryPath(path: DeliveryPath): Result<Unit> {
        val result = firestore.deleteDeliveryPath(path = path.toDataDeliveryPath())
        var finalResult: Result<Unit>? = null

        result.onSuccess {
            // Signal database update for delivery paths
            finalResult = saveNewDatabasePathsUpdate(System.currentTimeMillis())
        }

        return finalResult ?: result
    }

    ///////////////////////////////////////////////////////////////////////////
    // Order and Status Management
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves all orders and maps them from data models to domain models.
     */
    override suspend fun getAllOrders(onSuccess: (List<Order>?) -> Unit) {
        firestore.getAllOrders(onSuccess = { orders ->
            onSuccess.invoke(
                orders.map {
                    it.toOrder()
                }
            )
        }, onFailure = {
            onSuccess.invoke(
                null
            )
        })
    }

    /**
     * Retrieves preparation statuses and maps them from data models to domain models.
     */
    override suspend fun getPreparationStatuses(onSuccess: (List<PreparationStatus>?) -> Unit) {
        val result = firestore.getPreparationStatuses()
        result.onSuccess { list ->
            onSuccess(list.map { it.toDomain() })
        }
        result.onFailure {
            onSuccess(null)
        }
    }

    /**
     * Updates a preparation status in the database.
     */
    override suspend fun updatePreparationStatus(status: PreparationStatus): Result<Unit> {
        return firestore.updatePreparationStatus(status.toData())
    }

    ///////////////////////////////////////////////////////////////////////////
    // Global Update Triggers
    ///////////////////////////////////////////////////////////////////////////
    override suspend fun saveNewDatabaseProductsUpdate(timestamp: Long): Result<Unit> {
        return firestore.saveNewDatabaseProductUpdate(timestamp)
    }

    override suspend fun saveNewDatabasePathsUpdate(timestamp: Long): Result<Unit> {
        return firestore.saveNewDatabasePathsUpdate(timestamp)
    }
}