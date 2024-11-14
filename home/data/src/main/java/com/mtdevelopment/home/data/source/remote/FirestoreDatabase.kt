package com.mtdevelopment.home.data.source.remote

import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.mtdevelopment.home.data.model.ProductData
import com.mtdevelopment.home.data.model.toProductType

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
                onSuccess(snapshot.documents.map { item ->
                    ProductData(
                        id = item.id,
                        name = item.data?.get("name").toString(),
                        priceInCents = item.data?.get("priceCents") as? Long ?: 0L,
                        imageUrl = item.data?.get("imgUrl").toString(),
                        type = item.data?.get("type").toString().toProductType(),
                        description = item.data?.get("description").toString(),
                        allergens = item.data?.get("allergens") as? List<String> ?: emptyList(),
                    )
                })
            }.addOnFailureListener {
                Firebase.crashlytics.recordException(it)
                onFailure()
            }
    }

    fun getAllCheeses(onSuccess: (List<ProductData?>) -> Unit, onFailure: () -> Unit) {
        firestore.collection("products")
            .whereEqualTo("type", "cheese")
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.documents.map { item ->
                    item.toObject(ProductData::class.java)
                })
            }.addOnFailureListener {
                Firebase.crashlytics.recordException(it)
                onFailure()
            }
    }

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
}