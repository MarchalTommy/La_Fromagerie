package com.mtdevelopment.home.data.repository

import com.mtdevelopment.core.model.Product
import com.mtdevelopment.home.data.model.toProduct
import com.mtdevelopment.home.data.model.toProductEntity
import com.mtdevelopment.home.data.source.local.HomeDatabase
import com.mtdevelopment.home.domain.repository.RoomHomeRepository
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomHomeRepositoryImpl(
    private val homeDatabase: HomeDatabase
) : RoomHomeRepository {

    override suspend fun persistProduct(product: Product) {
        withContext(IO) {
            homeDatabase.persistProduct(product.toProductEntity())
        }
    }

    override suspend fun deleteProduct(product: Product) {
        withContext(IO) {
            homeDatabase.deleteProduct(product.toProductEntity())
        }
    }

    override suspend fun updateProduct(product: Product) {
        withContext(IO) {
            homeDatabase.updateProduct(product.toProductEntity())
        }
    }

    override suspend fun getProductById(id: String): Product? {
        return withContext(IO) {
            homeDatabase.getProductById(id).map { it?.toProduct() }.first()
        }
    }

    override suspend fun getCheeses(): List<Product> {
        return withContext(IO) {
            homeDatabase.getCheeses()
                .map { it.map { entity -> entity.toProduct() }.sortedBy { it.name } }.first()
        }
    }

    override suspend fun getProducts(): List<Product> {
        return withContext(IO) {
            homeDatabase.getAllProducts()
                .map { list ->
                    list.map { entity ->
                        entity.toProduct()
                    }.sortedBy { it.name }
                }.first()
        }
    }
}