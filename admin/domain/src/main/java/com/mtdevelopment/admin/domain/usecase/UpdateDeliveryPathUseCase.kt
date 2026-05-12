package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.DeliveryPath

/**
 * Use case to update an existing delivery path in the Firebase database.
 */
class UpdateDeliveryPathUseCase(
    private val firebaseRepository: FirebaseAdminRepository
) {
    /**
     * Executes the use case.
     * @param path The [DeliveryPath] object with updated information.
     * @param onSuccess Callback invoked when the update is successful.
     * @param onError Callback invoked when an error occurs during the update.
     */
    suspend operator fun invoke(
        path: DeliveryPath,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        firebaseRepository.updateDeliveryPath(path).onSuccess {
            onSuccess()
        }.onFailure {
            onError(it)
        }
    }
}