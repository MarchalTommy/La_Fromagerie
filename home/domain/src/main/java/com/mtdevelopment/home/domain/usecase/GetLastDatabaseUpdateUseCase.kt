package com.mtdevelopment.home.domain.usecase

import com.mtdevelopment.home.domain.repository.FirebaseHomeRepository

class GetLastDatabaseUpdateUseCase(
    private val firebaseHomeRepository: FirebaseHomeRepository
) {
    operator fun invoke(onSuccess: (Long) -> Unit, onFailure: () -> Unit) {
        firebaseHomeRepository.getLastDatabaseUpdate(onSuccess, onFailure)
    }
}