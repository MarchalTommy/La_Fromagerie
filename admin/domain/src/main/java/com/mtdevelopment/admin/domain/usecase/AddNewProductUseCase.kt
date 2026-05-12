package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.Product

/**
 * Use case to add a new product to the Firebase database.
 */
class AddNewProductUseCase(
    private val firebaseRepository: FirebaseAdminRepository
) {
    /**
     * Executes the use case.
     * @param product The [Product] to add.
     * @param onSuccess Callback invoked on success.
     * @param onError Callback invoked on failure.
     */
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