package com.mtdevelopment.home.domain.usecase

import com.mtdevelopment.home.domain.repository.FirebaseRepository

class GetLastDatabaseUpdateUseCase(
    private val firebaseRepository: FirebaseRepository
) {
    operator fun invoke(onSuccess: (Long) -> Unit, onFailure: () -> Unit) {
        firebaseRepository.getLastDatabaseUpdate(onSuccess, onFailure)
    }
}