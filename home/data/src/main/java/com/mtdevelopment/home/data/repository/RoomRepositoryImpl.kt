package com.mtdevelopment.home.data.repository

import com.mtdevelopment.home.data.model.toProduct
import com.mtdevelopment.home.data.model.toProductEntity
import com.mtdevelopment.home.data.source.local.HomeDatabaseDatasource
import com.mtdevelopment.home.domain.model.Product
import com.mtdevelopment.home.domain.repository.RoomRepository
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomRepositoryImpl(
    private val homeDatabaseDatasource: HomeDatabaseDatasource
) : RoomRepository {

    override suspend fun persistProduct(product: Product) {
        withContext(IO) {
            homeDatabaseDatasource.persistProduct(product.toProductEntity())
        }
    }

    override suspend fun deleteProduct(product: Product) {
        withContext(IO) {
            homeDatabaseDatasource.deleteProduct(product.toProductEntity())
        }
    }

    override suspend fun updateProduct(product: Product) {
        withContext(IO) {
            homeDatabaseDatasource.updateProduct(product.toProductEntity())
        }
    }

    override suspend fun getProductById(id: String, onSuccess: (Product) -> Unit) {
        onSuccess.invoke(withContext(IO) {
            homeDatabaseDatasource.getProductById(id).map { it.toProduct() }.first()
        }
        )
    }

    override suspend fun getCheeses(onSuccess: (List<Product>) -> Unit) {
        onSuccess.invoke(withContext(IO) {
            homeDatabaseDatasource.getCheeses().map { it.map { it.toProduct() }.sortedBy { it.name } }.first()
        })

    }

    override suspend fun getProducts(onSuccess: (List<Product>) -> Unit) {
        onSuccess.invoke(withContext(IO) {
            homeDatabaseDatasource.getAllProducts()
                .map { list ->
                    list.map { entity ->
                        entity.toProduct()
                    }.sortedBy { it.name }
                }.first()
        }
        )
    }
}