package com.mtdevelopment.admin.domain.repository

import com.mtdevelopment.core.model.DeliveryPath
import com.mtdevelopment.core.model.Product

interface FirebaseAdminRepository {

    suspend fun saveNewDatabaseProductsUpdate(timestamp: Long): Result<Unit>

    suspend fun addNewProduct(product: Product): Result<Unit>

    suspend fun updateProduct(product: Product): Result<Unit>

    suspend fun deleteProduct(product: Product): Result<Unit>

    suspend fun saveNewDatabasePathsUpdate(timestamp: Long): Result<Unit>

    suspend fun addNewDeliveryPath(path: DeliveryPath): Result<Unit>

    suspend fun updateDeliveryPath(path: DeliveryPath): Result<Unit>

    suspend fun deleteDeliveryPath(path: DeliveryPath): Result<Unit>

}