package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.Product

class UpdateProductUseCase(
    private val firebaseRepository: FirebaseAdminRepository
) {
    suspend operator fun invoke(
        product: Product,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        firebaseRepository.updateProduct(product).onSuccess {
            onSuccess()
        }.onFailure {
            onError(it)
        }
    }
}