package com.mtdevelopment.admin.data.repository

import com.mtdevelopment.admin.data.model.toDataDeliveryPath
import com.mtdevelopment.admin.data.source.FirestoreAdminDatasource
import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.DeliveryPath
import com.mtdevelopment.core.model.toProductData

class FirebaseAdminRepositoryImpl(
    private val firestore: FirestoreAdminDatasource
) : FirebaseAdminRepository {

    ///////////////////////////////////////////////////////////////////////////
    // Product
    ///////////////////////////////////////////////////////////////////////////
    override fun addNewProduct(product: com.mtdevelopment.core.model.Product) {
        firestore.addNewProduct(product = product.toProductData())
        saveNewDatabaseProductsUpdate(System.currentTimeMillis())
    }

    override fun updateProduct(product: com.mtdevelopment.core.model.Product) {
        firestore.updateProduct(product = product.toProductData())
        saveNewDatabaseProductsUpdate(System.currentTimeMillis())
    }

    override fun deleteProduct(product: com.mtdevelopment.core.model.Product) {
        firestore.deleteProduct(product = product.toProductData())
        saveNewDatabaseProductsUpdate(System.currentTimeMillis())
    }

    //////////////////////////////////////////////////////////////////////////
    // Delivery Paths
    ///////////////////////////////////////////////////////////////////////////
    override fun addNewDeliveryPath(path: DeliveryPath) {
        firestore.addNewDeliveryPath(path = path.toDataDeliveryPath())
        saveNewDatabasePathsUpdate(System.currentTimeMillis())
    }

    override fun updateDeliveryPath(path: DeliveryPath) {
        firestore.updateDeliveryPath(path = path.toDataDeliveryPath())
        saveNewDatabasePathsUpdate(System.currentTimeMillis())
    }

    override fun deleteDeliveryPath(path: DeliveryPath) {
        firestore.deleteDeliveryPath(path = path.toDataDeliveryPath())
        saveNewDatabasePathsUpdate(System.currentTimeMillis())
    }

    ///////////////////////////////////////////////////////////////////////////
    // Database Update Timestamp
    ///////////////////////////////////////////////////////////////////////////
    override fun saveNewDatabaseProductsUpdate(timestamp: Long) {
        firestore.saveNewDatabaseProductUpdate(timestamp)
    }

    override fun saveNewDatabasePathsUpdate(timestamp: Long) {
        firestore.saveNewDatabasePathsUpdate(timestamp)
    }

}