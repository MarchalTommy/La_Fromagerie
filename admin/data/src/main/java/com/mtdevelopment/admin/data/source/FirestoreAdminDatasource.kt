package com.mtdevelopment.admin.data.source

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mtdevelopment.admin.data.model.DataDeliveryPath
import com.mtdevelopment.core.model.ProductData
import kotlinx.coroutines.tasks.await
import java.time.Instant

class FirestoreAdminDatasource(
    private val firestore: FirebaseFirestore
) {

    ///////////////////////////////////////////////////////////////////////////
    // PRODUCTS
    ///////////////////////////////////////////////////////////////////////////
    suspend fun addNewProduct(product: ProductData): Result<Unit> {
        return try {
            firestore.collection("products")
                .add(product)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveImageUrlToFirestore(
        cloudinaryUrl: String,
        productId: String,
        collectionPath: String
    ): Result<Unit> {
        return try {
            val firestore = Firebase.firestore
            val documentData = mapOf(
                "imgUrl" to cloudinaryUrl
            )
            firestore.collection(collectionPath).document(productId)
                .update(documentData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProduct(product: ProductData): Result<Unit> {
        return try {
            firestore.collection("products")
                .document(product.id)
                .set(product)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(product: ProductData): Result<Unit> {
        return try {
            firestore.collection("products")
                .document(product.id)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveNewDatabaseProductUpdate(timestamp: Long): Result<Unit> {
        return try {
            firestore.collection("database_update")
                .document("products_timestamp")
                .set(mapOf("last_update" to Timestamp(Instant.ofEpochMilli(timestamp))))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // DELIVERY PATH
    ///////////////////////////////////////////////////////////////////////////
    suspend fun addNewDeliveryPath(path: DataDeliveryPath): Result<Unit> {
        return try {
            firestore.collection("delivery_paths")
                .add(path)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDeliveryPath(path: DataDeliveryPath): Result<Unit> {
        return try {
            firestore.collection("delivery_paths")
                .document(path.id)
                .set(path)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDeliveryPath(path: DataDeliveryPath): Result<Unit> {
        return try {
            firestore.collection("delivery_paths")
                .document(path.id)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveNewDatabasePathsUpdate(timestamp: Long): Result<Unit> {
        return try {
            firestore.collection("database_update")
                .document("path_timestamp")
                .set(mapOf("last_update" to Timestamp(Instant.ofEpochMilli(timestamp))))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}