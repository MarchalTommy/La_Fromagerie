package com.mtdevelopment.home.domain.repository

import com.mtdevelopment.core.model.Product

interface FirebaseHomeRepository {
    // TODO: SAVE ALL ORDERS
//    fun saveOrder(order: Order)

    fun getAllProducts(onSuccess: (List<Product>) -> Unit, onFailure: () -> Unit)

    fun getAllCheeses(onSuccess: (List<Product>) -> Unit, onFailure: () -> Unit)

    fun getLastDatabaseUpdate(onSuccess: (Long) -> Unit, onFailure: () -> Unit)

}