package com.mtdevelopment.home.domain.usecase

import com.mtdevelopment.home.domain.model.Product
import com.mtdevelopment.home.domain.repository.FirebaseRepository

class GetAllCheesesUseCase(
    private val firebaseRepository: FirebaseRepository
) {
    operator fun invoke(onSuccess: (List<Product>) -> Unit, onFailure: () -> Unit) {
        firebaseRepository.getAllCheeses(onSuccess, onFailure)
    }
}