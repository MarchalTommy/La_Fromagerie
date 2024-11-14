package com.mtdevelopment.home.data.repository

import com.mtdevelopment.home.data.model.toProduct
import com.mtdevelopment.home.data.model.toProductData
import com.mtdevelopment.home.data.source.remote.FirestoreDatabase
import com.mtdevelopment.home.domain.model.Product
import com.mtdevelopment.home.domain.repository.FirebaseRepository

class FirebaseRepositoryImpl(
    private val firestore: FirestoreDatabase
) : FirebaseRepository {

    override fun getAllProducts(onSuccess: (List<Product>) -> Unit, onFailure: () -> Unit) {
        firestore.getAllProducts(onSuccess = {
            onSuccess.invoke(it.mapNotNull { item -> item?.toProduct() })
        }, onFailure)
    }

    override fun getAllCheeses(onSuccess: (List<Product>) -> Unit, onFailure: () -> Unit) {
        firestore.getAllProducts(onSuccess = {
            onSuccess.invoke(it.mapNotNull { item -> item?.toProduct() })
        }, onFailure)
    }

    override fun addNewProduct(product: Product) {
        firestore.addNewProduct(product = product.toProductData())
    }

    override fun updateProduct(product: Product) {
        firestore.updateProduct(product = product.toProductData())
    }

    override fun deleteProduct(product: Product) {
        firestore.deleteProduct(product = product.toProductData())
    }

}