package com.mtdevelopment.home.domain.repository

import com.mtdevelopment.home.domain.model.Product

interface RoomRepository {
    suspend fun persistProduct(product: Product)
    suspend fun deleteProduct(product: Product)
    suspend fun updateProduct(product: Product)
    suspend fun getProductById(id: String, onSuccess: (Product) -> Unit)
    suspend fun getCheeses(onSuccess: (List<Product>) -> Unit)
    suspend fun getProducts(onSuccess: (List<Product>) -> Unit)
}