package com.mtdevelopment.home.data.source.local

import com.mtdevelopment.home.data.model.ProductEntity
import com.mtdevelopment.home.data.source.local.dao.HomeDao

class HomeDatabaseDatasource(
    private val dao: HomeDao
) {

    suspend fun persistProduct(product: ProductEntity) {
        dao.persistProduct(product)
    }

    suspend fun deleteProduct(product: ProductEntity) {
        dao.deleteProduct(product)
    }

    suspend fun updateProduct(product: ProductEntity) {
        dao.updateProduct(product)
    }

    fun getProductById(id: String) = dao.getProductById(id)

    fun getCheeses() = dao.getCheeses()

    fun getAllProducts() = dao.getAllProducts()

}