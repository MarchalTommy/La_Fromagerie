package com.mtdevelopment.home.data.repository

import com.mtdevelopment.core.model.toProduct
import com.mtdevelopment.core.model.toProductData
import com.mtdevelopment.home.data.source.remote.FirestoreDatabase
import com.mtdevelopment.core.model.Product
import com.mtdevelopment.home.domain.repository.FirebaseHomeRepository

class FirebaseHomeRepositoryImpl(
    private val firestore: FirestoreDatabase
) : FirebaseHomeRepository {

    override fun getAllProducts(onSuccess: (List<Product>) -> Unit, onFailure: () -> Unit) {
        firestore.getAllProducts(onSuccess = {
            onSuccess.invoke(it.mapNotNull { item -> item?.toProduct() }.sortedBy { it.name })
        }, onFailure)
    }

    override fun getAllCheeses(onSuccess: (List<Product>) -> Unit, onFailure: () -> Unit) {
        firestore.getAllProducts(onSuccess = {
            onSuccess.invoke(it.mapNotNull { item -> item?.toProduct() }.sortedBy { it.name })
        }, onFailure)
    }

    override fun getLastDatabaseUpdate(onSuccess: (Long) -> Unit, onFailure: () -> Unit) {
        firestore.getLastDatabaseUpdate(onSuccess = { timestamp ->
            val result = timestamp.toInstant().toEpochMilli()
            onSuccess.invoke(result)
        }, onFailure = {
            onFailure.invoke()
        })
    }
}