package com.mtdevelopment.core.usecase

import com.mtdevelopment.core.repository.SharedDatastore

class ClearDatastoreUseCase(
    private val sharedDatastore: SharedDatastore
) {
    suspend operator fun invoke() {
        sharedDatastore.clearAllDatastore()
    }
}