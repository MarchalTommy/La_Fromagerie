package com.mtdevelopment.admin.domain.repository

import com.mtdevelopment.core.model.DeliveryPath
import com.mtdevelopment.core.model.Product

interface FirebaseAdminRepository {

    fun saveNewDatabaseProductsUpdate(timestamp: Long)

    fun addNewProduct(product: Product)

    fun updateProduct(product: Product)

    fun deleteProduct(product: Product)

    fun saveNewDatabasePathsUpdate(timestamp: Long)

    fun addNewDeliveryPath(path: DeliveryPath)

    fun updateDeliveryPath(path: DeliveryPath)

    fun deleteDeliveryPath(path: DeliveryPath)

}