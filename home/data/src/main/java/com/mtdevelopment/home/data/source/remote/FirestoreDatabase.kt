package com.mtdevelopment.home.data.source.remote

import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.mtdevelopment.core.model.ProductData
import com.mtdevelopment.core.model.toProductType
import com.mtdevelopment.home.data.model.FirestoreUpdateData
import kotlinx.coroutines.tasks.await

class FirestoreDatabase(
    private val firestore: FirebaseFirestore
) {

    suspend fun getAllProducts(): List<ProductData> {
        return try {
            val snapshot = firestore.collection("products").get().await()
            snapshot.documents.map { item ->
                if (item.data?.get("name") == null) {
                    ProductData(
                        id = item.id,
                        name = item.data?.get("b").toString(),
                        priceCents = item.data?.get("c") as? Long ?: 0L,
                        imgUrl = item.data?.get("d").toString(),
                        type = item.data?.get("e").toString().toProductType(),
                        description = item.data?.get("f").toString(),
                        allergens = item.data?.get("g") as? List<String> ?: emptyList(),
                        isAvailable = item.data?.get("h") as? Boolean != false
                    )
                } else {
                    ProductData(
                        id = item.id,
                        name = item.data?.get("name").toString(),
                        priceCents = item.data?.get("priceCents") as? Long ?: 0L,
                        imgUrl = item.data?.get("imgUrl").toString(),
                        type = item.data?.get("type").toString().toProductType(),
                        description = item.data?.get("description").toString(),
                        allergens = item.data?.get("allergens") as? List<String> ?: emptyList(),
                        isAvailable = item.data?.get("available") as? Boolean != false
                    )
                }
            }
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
            throw e
        }
    }

    suspend fun getAllCheeses(): List<ProductData?> {
        return try {
            val snapshot = firestore.collection("products")
                .whereEqualTo("type", "cheese")
                .get()
                .await()
            snapshot.documents.map { item ->
                item.toObject(ProductData::class.java)
            }
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
            throw e
        }
    }

    suspend fun getLastDatabaseUpdate(): FirestoreUpdateData {
        return try {
            val snapshot = firestore.collection("database_update").get().await()
            var productTimestamp = 0L
            var pathTimestamp = 0L
            snapshot.documents.forEach { document ->
                if (document.id == "products_timestamp") {
                    productTimestamp =
                        (document.data?.get("last_update") as? Timestamp)?.toInstant()
                            ?.toEpochMilli() ?: 0L
                }
                if (document.id == "path_timestamp") {
                    pathTimestamp = (document.data?.get("last_update") as? Timestamp)?.toInstant()
                        ?.toEpochMilli() ?: 0L
                }
            }
            FirestoreUpdateData(
                productsTimestamp = productTimestamp,
                pathsTimestamp = pathTimestamp
            )
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
            throw e
        }
    }
}