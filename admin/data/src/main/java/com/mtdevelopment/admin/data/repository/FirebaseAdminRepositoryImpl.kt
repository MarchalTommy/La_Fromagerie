package com.mtdevelopment.admin.data.repository

import com.mtdevelopment.admin.data.model.toDataDeliveryPath
import com.mtdevelopment.admin.data.source.FirestoreAdminDatasource
import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.DeliveryPath
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.toOrder
import com.mtdevelopment.core.model.toProductData

class FirebaseAdminRepositoryImpl(
    private val firestore: FirestoreAdminDatasource
) : FirebaseAdminRepository {

    ///////////////////////////////////////////////////////////////////////////
    // Product
    ///////////////////////////////////////////////////////////////////////////
    override suspend fun addNewProduct(product: com.mtdevelopment.core.model.Product): Result<Unit> {
        val result = firestore.addNewProduct(product = product.toProductData())
        var finalResult: Result<Unit>? = null

        result.onSuccess {
            finalResult = saveNewDatabaseProductsUpdate(System.currentTimeMillis())
        }

        return finalResult ?: result
    }

    override suspend fun updateProduct(product: com.mtdevelopment.core.model.Product): Result<Unit> {
        val result = firestore.updateProduct(product = product.toProductData())
        var finalResult: Result<Unit>? = null

        result.onSuccess {
            finalResult = saveNewDatabaseProductsUpdate(System.currentTimeMillis())
        }

        return finalResult ?: result
    }

    override suspend fun deleteProduct(product: com.mtdevelopment.core.model.Product): Result<Unit> {
        val result = firestore.deleteProduct(product = product.toProductData())
        var finalResult: Result<Unit>? = null

        result.onSuccess {
            finalResult = saveNewDatabaseProductsUpdate(System.currentTimeMillis())
        }

        return finalResult ?: result
    }

    //////////////////////////////////////////////////////////////////////////
    // Delivery Paths
    ///////////////////////////////////////////////////////////////////////////
    override suspend fun addNewDeliveryPath(path: DeliveryPath): Result<Unit> {
        val result = firestore.addNewDeliveryPath(path = path.toDataDeliveryPath())
        var finalResult: Result<Unit>? = null

        result.onSuccess {
            finalResult = saveNewDatabasePathsUpdate(System.currentTimeMillis())
        }

        return finalResult ?: result
    }

    override suspend fun updateDeliveryPath(path: DeliveryPath): Result<Unit> {
        val result = firestore.updateDeliveryPath(path = path.toDataDeliveryPath())
        var finalResult: Result<Unit>? = null

        result.onSuccess {
            finalResult = saveNewDatabasePathsUpdate(System.currentTimeMillis())
        }

        return finalResult ?: result
    }

    override suspend fun deleteDeliveryPath(path: DeliveryPath): Result<Unit> {
        val result = firestore.deleteDeliveryPath(path = path.toDataDeliveryPath())
        var finalResult: Result<Unit>? = null

        result.onSuccess {
            finalResult = saveNewDatabasePathsUpdate(System.currentTimeMillis())
        }

        return finalResult ?: result
    }

    ///////////////////////////////////////////////////////////////////////////
    // ORDERS
    ///////////////////////////////////////////////////////////////////////////

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

    ///////////////////////////////////////////////////////////////////////////
    // Database Update Timestamp
    ///////////////////////////////////////////////////////////////////////////
    override suspend fun saveNewDatabaseProductsUpdate(timestamp: Long): Result<Unit> {
        return firestore.saveNewDatabaseProductUpdate(timestamp)
    }

    override suspend fun saveNewDatabasePathsUpdate(timestamp: Long): Result<Unit> {
        return firestore.saveNewDatabasePathsUpdate(timestamp)
    }

}