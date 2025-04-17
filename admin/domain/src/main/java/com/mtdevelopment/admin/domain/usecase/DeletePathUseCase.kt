package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.DeliveryPath

class DeletePathUseCase(
    private val firebaseRepository: FirebaseAdminRepository
) {

    suspend operator fun invoke(
        path: DeliveryPath,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        firebaseRepository.deleteDeliveryPath(path).onSuccess {
            onSuccess()
        }.onFailure {
            onError(it)
        }
    }

}