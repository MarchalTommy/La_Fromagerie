package com.mtdevelopment.core.usecase

import com.mtdevelopment.core.repository.SharedDatastore

class ClearOrderUseCase(
    private val sharedDatastore: SharedDatastore
) {
    suspend operator fun invoke() {
        sharedDatastore.clearOrder()
    }
}