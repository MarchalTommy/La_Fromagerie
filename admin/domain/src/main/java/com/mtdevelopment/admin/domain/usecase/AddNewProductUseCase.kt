package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.Product

class AddNewProductUseCase(
    private val firebaseRepository: FirebaseAdminRepository
) {
    suspend operator fun invoke(
        product: Product,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        firebaseRepository.addNewProduct(product).onSuccess {
            onSuccess()
        }.onFailure {
            onError(it)
        }
    }
}