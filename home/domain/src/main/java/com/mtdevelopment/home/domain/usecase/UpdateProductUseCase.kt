package com.mtdevelopment.home.domain.usecase

import com.mtdevelopment.home.domain.model.Product
import com.mtdevelopment.home.domain.repository.FirebaseRepository

class UpdateProductUseCase(
    private val firebaseRepository: FirebaseRepository
) {
    operator fun invoke(product: Product) {
        firebaseRepository.updateProduct(product)
    }
}