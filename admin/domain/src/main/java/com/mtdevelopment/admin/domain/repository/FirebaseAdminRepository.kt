package com.mtdevelopment.admin.domain.repository

import com.mtdevelopment.core.model.Product

interface FirebaseAdminRepository {

    fun saveNewDatabaseUpdate(timestamp: Long)

    fun addNewProduct(product: Product)

    fun updateProduct(product: Product)

    fun deleteProduct(product: Product)

}