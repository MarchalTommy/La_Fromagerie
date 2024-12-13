package com.mtdevelopment.admin.data.source

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.mtdevelopment.admin.data.model.DataDeliveryPath
import com.mtdevelopment.core.model.ProductData
import java.time.Instant

class FirestoreAdminDatasource(
    private val firestore: FirebaseFirestore
) {

    ///////////////////////////////////////////////////////////////////////////
    // PRODUCTS
    ///////////////////////////////////////////////////////////////////////////
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

    fun saveNewDatabaseProductUpdate(timestamp: Long) {
        firestore.collection("database_update")
            .document("products_timestamp")
            .set(mapOf("last_update" to Timestamp(Instant.ofEpochMilli(timestamp))))
    }

    ///////////////////////////////////////////////////////////////////////////
    // DELIVERY PATH
    ///////////////////////////////////////////////////////////////////////////
    fun addNewDeliveryPath(path: DataDeliveryPath) {
        firestore.collection("delivery_paths")
            .add(path)
    }

    fun updateDeliveryPath(path: DataDeliveryPath) {
        firestore.collection("delivery_paths")
            .document(path.id)
            .set(path)
    }

    fun saveNewDatabasePathsUpdate(timestamp: Long) {
        firestore.collection("database_update")
            .document("path_timestamp")
            .set(mapOf("last_update" to Timestamp(Instant.ofEpochMilli(timestamp))))
    }
}