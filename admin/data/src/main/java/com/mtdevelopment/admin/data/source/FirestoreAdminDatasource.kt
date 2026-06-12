package com.mtdevelopment.admin.data.source

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.mtdevelopment.admin.data.model.DataDeliveryPath
import com.mtdevelopment.core.model.OrderData
import com.mtdevelopment.core.model.OrderStatus
import com.mtdevelopment.core.model.PreparationStatusData
import com.mtdevelopment.core.model.ProductData
import kotlinx.coroutines.tasks.await
import java.time.Instant

/**
 * Data source for administrative Firestore operations.
 * It provides methods for managing products, delivery paths, orders, and preparation statuses.
 * Most methods are [suspend] and use [await] for better coroutine integration.
 */
class FirestoreAdminDatasource(
    private val firestore: FirebaseFirestore
) {

    ///////////////////////////////////////////////////////////////////////////
    // Product Management
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

    /**
     * Updates only the image URL for a specific document.
     */
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

    /**
     * Updates the global timestamp for product changes.
     */
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
    // Delivery Path Management
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

    /**
     * Updates the global timestamp for delivery path changes.
     */
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

    ///////////////////////////////////////////////////////////////////////////
    // Order Management
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Fetches all orders from the "orders" collection.
     *
     * Kept callback-based on purpose: its consumers (admin view models) plug UI callbacks
     * straight into it, and converting it to a suspend function would ripple through the whole
     * admin call chain for no behavioral gain. Manual mapping is also intentional: the documents
     * use snake_case keys and a string status that needs an explicit [OrderStatus] conversion,
     * which [com.google.firebase.firestore.DocumentSnapshot.toObject] cannot do here.
     *
     * Documents that cannot be mapped (e.g. an unknown status written by a newer app version)
     * are skipped instead of failing the entire list.
     */
    fun getAllOrders(
        onSuccess: (List<OrderData>) -> Unit,
        onFailure: () -> Unit
    ) {
        firestore.collection("orders")
            .get()
            .addOnFailureListener {
                onFailure.invoke()
            }
            .addOnSuccessListener { snapshot ->
                // A throw inside this listener would crash the app: map defensively
                // and route any malformed document to the failure callback instead.
                try {
                    onSuccess.invoke(snapshot.documents.map { item ->
                        OrderData(
                            id = item.id,
                            customer_name = item.data?.get("customer_name").toString(),
                            customer_address = item.data?.get("customer_address").toString(),
                            delivery_date = item.data?.get("delivery_date").toString(),
                            order_date = item.data?.get("order_date").toString(),
                            // Firestore stores numbers as Long: an unchecked cast to
                            // Map<String, Int> would throw ClassCastException at read time.
                            products = (item.data?.get("products") as? Map<*, *>)
                                ?.entries
                                ?.mapNotNull { (key, value) ->
                                    val name = key as? String ?: return@mapNotNull null
                                    val quantity =
                                        (value as? Number)?.toInt() ?: return@mapNotNull null
                                    name to quantity
                                }?.toMap() ?: emptyMap(),
                            status = runCatching {
                                OrderStatus.valueOf(item.data?.get("status").toString())
                            }.getOrDefault(OrderStatus.PENDING),
                            note = item.data?.get("note") as? String,
                            billing_address = item.data?.get("billing_address").toString(),
                            is_manually_added = item.data?.get("is_manually_added") as? Boolean
                        )
                    })
                } catch (e: Exception) {
                    onFailure.invoke()
                }
            }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Preparation Status Management
    ///////////////////////////////////////////////////////////////////////////
    suspend fun getPreparationStatuses(): Result<List<PreparationStatusData>> {
        return try {
            val result = firestore.collection("preparation_status")
                .get()
                .await()
            val list = result.documents.mapNotNull { it.toObject(PreparationStatusData::class.java) }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePreparationStatus(status: PreparationStatusData): Result<Unit> {
        return try {
            firestore.collection("preparation_status")
                .document(status.id)
                .set(status)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}