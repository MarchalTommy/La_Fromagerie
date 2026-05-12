package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.Product

/**
 * Use case to update an existing product in the Firebase database.
 */
class UpdateProductUseCase(
    private val firebaseRepository: FirebaseAdminRepository
) {
    /**
     * Executes the use case.
     * @param product The [Product] with updated information.
     * @param onSuccess Callback invoked on success.
     * @param onError Callback invoked on failure.
     */
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