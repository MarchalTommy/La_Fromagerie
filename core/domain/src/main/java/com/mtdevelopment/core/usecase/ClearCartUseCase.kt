package com.mtdevelopment.core.usecase

import com.mtdevelopment.core.repository.SharedDatastore

/**
 * Use case to completely empty the shopping cart by clearing the persistent storage.
 */
class ClearCartUseCase(
    private val sharedDatastore: SharedDatastore
) {
    /**
     * Executes the use case.
     */
    suspend operator fun invoke() {
        sharedDatastore.clearCartItems()
    }
}