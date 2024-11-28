package com.mtdevelopment.home.data.source.remote

import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.mtdevelopment.core.model.ProductData
import com.mtdevelopment.core.model.toProductType
import com.mtdevelopment.home.data.model.FirestoreUpdateData

class FirestoreDatabase(
    private val firestore: FirebaseFirestore
) {
//    fun saveOrder(order: Order) {
//        firestore.collection("orders").add(order)
//    }

    fun getAllProducts(onSuccess: (List<ProductData?>) -> Unit, onFailure: () -> Unit) {
        firestore.collection("products")
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess.invoke(snapshot.documents.map { item ->
                    if (item.data?.get("name") == null) {
                        ProductData(
                            id = item.id,
                            name = item.data?.get("b").toString(),
                            priceCents = item.data?.get("c") as? Long ?: 0L,
                            imgUrl = item.data?.get("d").toString(),
                            type = item.data?.get("e").toString().toProductType(),
                            description = item.data?.get("f").toString(),
                            allergens = item.data?.get("g") as? List<String> ?: emptyList(),
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
                        )
                    }
                })
            }.addOnFailureListener {
                Firebase.crashlytics.recordException(it)
                onFailure.invoke()
            }
    }

    fun getAllCheeses(onSuccess: (List<ProductData?>) -> Unit, onFailure: () -> Unit) {
        firestore.collection("products")
            .whereEqualTo("type", "cheese")
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess.invoke(snapshot.documents.map { item ->
                    item.toObject(ProductData::class.java)
                })
            }.addOnFailureListener {
                Firebase.crashlytics.recordException(it)
                onFailure.invoke()
            }
    }

    fun getLastDatabaseUpdate(onSuccess: (FirestoreUpdateData) -> Unit, onFailure: () -> Unit) {
        firestore.collection("database_update")
            .get()
            .addOnSuccessListener { snapshot ->
                var productTimestamp = 0L
                var pathTimestamp = 0L
                snapshot.documents.map { document ->
                    if (document.id == "products_timestamp") productTimestamp =
                        (document.data?.get("last_update") as? Timestamp)?.toInstant()
                            ?.toEpochMilli()
                            ?: 0L

                    if (document.id == "path_timestamp") pathTimestamp =
                        (document.data?.get("last_update") as? Timestamp)?.toInstant()
                            ?.toEpochMilli()
                            ?: 0L
                }
                onSuccess.invoke(
                    FirestoreUpdateData(
                        productsTimestamp = productTimestamp,
                        pathsTimestamp = pathTimestamp
                    )
                )
            }.addOnFailureListener {
                onFailure.invoke()
            }
    }
}