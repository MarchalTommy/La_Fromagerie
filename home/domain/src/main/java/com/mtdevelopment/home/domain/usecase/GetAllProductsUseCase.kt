package com.mtdevelopment.home.domain.usecase

import com.mtdevelopment.home.domain.model.Product
import com.mtdevelopment.home.domain.repository.FirebaseRepository

class GetAllProductsUseCase(
    private val firebaseRepository: FirebaseRepository
) {
    operator fun invoke(onSuccess: (List<Product>) -> Unit, onFailure: () -> Unit) {
        firebaseRepository.getAllProducts(onSuccess, onFailure)
    }
}