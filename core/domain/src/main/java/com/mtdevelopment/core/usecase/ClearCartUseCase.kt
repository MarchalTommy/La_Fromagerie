package com.mtdevelopment.core.usecase

import com.mtdevelopment.core.repository.SharedDatastore

class ClearCartUseCase(
    private val sharedDatastore: SharedDatastore
) {
    suspend operator fun invoke() {
        sharedDatastore.clearCartItems()
    }
}