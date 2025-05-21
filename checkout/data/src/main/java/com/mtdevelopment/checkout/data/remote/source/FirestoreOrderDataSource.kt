package com.mtdevelopment.checkout.data.remote.source

import com.google.firebase.firestore.FirebaseFirestore
import com.mtdevelopment.core.model.OrderData
import com.mtdevelopment.core.model.OrderStatus
import kotlinx.coroutines.tasks.await

class FirestoreOrderDataSource(
    private val firestore: FirebaseFirestore
) {
    suspend fun createOrder(
        orderData: OrderData
    ): Result<Unit> {
        return try {
            firestore.collection("orders")
                .document(orderData.id)
                .set(orderData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrder(
        orderId: String,
        newStatus: OrderStatus
    ): Result<Unit> {
        return try {
            firestore.collection("orders")
                .document(orderId)
                .update("status", newStatus.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}