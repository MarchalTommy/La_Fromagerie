package com.mtdevelopment.home.data.repository

import com.mtdevelopment.core.model.Product
import com.mtdevelopment.core.model.toProduct
import com.mtdevelopment.core.util.DataResult
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
    override suspend fun getAllProducts(): DataResult<List<Product>> {
        return try {
            val products = firestore.getAllProducts()
            DataResult.Success(products.map { it.toProduct() }.sortedBy { it.name })
        } catch (e: Exception) {
            DataResult.Error(e, "Chargement des produits impossible")
        }
    }

    /**
     * Currently retrieves all products (same as [getAllProducts]). 
     * // TODO: Implement specific filtering for cheeses if the database supports it.
     */
    override suspend fun getAllCheeses(): DataResult<List<Product>> {
        return try {
            val cheeses = firestore.getAllCheeses()
            DataResult.Success(cheeses.mapNotNull { it?.toProduct() }.sortedBy { it.name })
        } catch (e: Exception) {
            DataResult.Error(e, "Chargement des fromages impossible")
        }
    }

    /**
     * Fetches the global database update timestamps from Firestore.
     */
    override suspend fun getLastDatabaseUpdate(): DataResult<Pair<Long, Long>> {
        return try {
            val timestamp = firestore.getLastDatabaseUpdate()
            DataResult.Success(Pair(timestamp.productsTimestamp, timestamp.pathsTimestamp))
        } catch (e: Exception) {
            DataResult.Error(e, "Mise à jour de la base de donnée impossible")
        }
    }
}