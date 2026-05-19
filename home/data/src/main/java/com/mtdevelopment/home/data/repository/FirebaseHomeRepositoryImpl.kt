package com.mtdevelopment.home.data.repository

import com.mtdevelopment.core.model.Product
import com.mtdevelopment.core.model.toProduct
import com.mtdevelopment.home.data.source.remote.FirestoreDatabase
import com.mtdevelopment.home.domain.repository.FirebaseHomeRepository

/**
 * Implementation of [FirebaseHomeRepository] that fetches product and synchronization data 
 * from the [FirestoreDatabase] source.
 */
class FirebaseHomeRepositoryImpl(
    private val firestore: FirestoreDatabase
) : FirebaseHomeRepository {

    /**
     * Retrieves all products and maps them from Firestore DTOs to domain [Product] models.
     * Results are sorted alphabetically by name.
     */
    override fun getAllProducts(onSuccess: (List<Product>) -> Unit, onFailure: () -> Unit) {
        firestore.getAllProducts(onSuccess = {
            onSuccess.invoke(it.mapNotNull { item -> item?.toProduct() }.sortedBy { it.name })
        }, onFailure)
    }

    /**
     * Currently retrieves all products (same as [getAllProducts]). 
     * // TODO: Implement specific filtering for cheeses if the database supports it, 
     * // or filter the list locally if needed.
     */
    override fun getAllCheeses(onSuccess: (List<Product>) -> Unit, onFailure: () -> Unit) {
        firestore.getAllProducts(onSuccess = {
            onSuccess.invoke(it.mapNotNull { item -> item?.toProduct() }.sortedBy { it.name })
        }, onFailure)
    }

    /**
     * Fetches the global database update timestamps from Firestore.
     */
    override fun getLastDatabaseUpdate(
        onSuccess: (products: Long, paths: Long) -> Unit,
        onFailure: () -> Unit
    ) {
        firestore.getLastDatabaseUpdate(onSuccess = { timestamp ->
            val productResult = timestamp.productsTimestamp
            val pathResult = timestamp.pathsTimestamp
            onSuccess(productResult, pathResult)
        }, onFailure = {
            onFailure.invoke()
        })
    }
}