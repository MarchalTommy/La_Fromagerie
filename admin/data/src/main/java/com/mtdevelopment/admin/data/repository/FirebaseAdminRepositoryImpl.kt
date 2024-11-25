package com.mtdevelopment.admin.data.repository

import com.mtdevelopment.admin.data.source.FirestoreAdminDatasource
import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.toProductData

class FirebaseAdminRepositoryImpl(
    private val firestore: FirestoreAdminDatasource
) : FirebaseAdminRepository {


    override fun addNewProduct(product: com.mtdevelopment.core.model.Product) {
        firestore.addNewProduct(product = product.toProductData())
        saveNewDatabaseUpdate(System.currentTimeMillis())
    }

    override fun updateProduct(product: com.mtdevelopment.core.model.Product) {
        firestore.updateProduct(product = product.toProductData())
        saveNewDatabaseUpdate(System.currentTimeMillis())
    }

    override fun deleteProduct(product: com.mtdevelopment.core.model.Product) {
        firestore.deleteProduct(product = product.toProductData())
        saveNewDatabaseUpdate(System.currentTimeMillis())
    }

    override fun saveNewDatabaseUpdate(timestamp: Long) {
        firestore.saveNewDatabaseUpdate(timestamp)
    }

}