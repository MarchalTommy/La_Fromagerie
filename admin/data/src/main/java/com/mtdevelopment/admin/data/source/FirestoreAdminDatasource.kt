package com.mtdevelopment.admin.data.source

import com.google.firebase.firestore.FirebaseFirestore
import com.mtdevelopment.core.model.*

class FirestoreAdminDatasource(
    private val firestore: FirebaseFirestore
) {

    fun addNewProduct(product: ProductData) {
        firestore.collection("products")
            .add(product)
    }

    fun updateProduct(product: ProductData) {
        firestore.collection("products")
            .document(product.id)
            .set(product)
    }

    fun deleteProduct(product: ProductData) {
        firestore.collection("products")
            .document(product.id)
            .delete()
    }

    fun saveNewDatabaseUpdate(timestamp: Long) {
        firestore.collection("database_update")
            .document("last_database_update")
            .set(mapOf("timestamp" to timestamp))

    }
}