package com.mtdevelopment.home.domain.repository

import com.mtdevelopment.core.model.Product

interface FirebaseHomeRepository {

    fun getAllProducts(onSuccess: (List<Product>) -> Unit, onFailure: () -> Unit)

    fun getAllCheeses(onSuccess: (List<Product>) -> Unit, onFailure: () -> Unit)

    fun getLastDatabaseUpdate(onSuccess: (products: Long, paths: Long) -> Unit, onFailure: () -> Unit)

}