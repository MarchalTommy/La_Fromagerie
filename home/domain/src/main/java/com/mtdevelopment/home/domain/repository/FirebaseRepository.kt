package com.mtdevelopment.home.domain.repository

import com.mtdevelopment.home.domain.model.Product

interface FirebaseRepository {
    // TODO: SAVE ALL ORDERS
//    fun saveOrder(order: Order)

    fun getAllProducts(onSuccess: (List<Product>) -> Unit, onFailure: () -> Unit)

    fun getAllCheeses(onSuccess: (List<Product>) -> Unit, onFailure: () -> Unit)

    fun addNewProduct(product: Product)

    fun updateProduct(product: Product)

    fun deleteProduct(product: Product)

    fun getLastDatabaseUpdate(onSuccess: (Long) -> Unit, onFailure: () -> Unit)

    fun saveNewDatabaseUpdate(timestamp: Long)

}