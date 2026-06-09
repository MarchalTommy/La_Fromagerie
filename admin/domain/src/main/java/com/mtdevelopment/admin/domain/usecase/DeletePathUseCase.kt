package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.DeliveryPath

/**
 * Use case to delete a delivery path from the Firebase database.
 */
class DeletePathUseCase(
    private val firebaseRepository: FirebaseAdminRepository
) {

    /**
     * Executes the use case.
     * @param path The [DeliveryPath] to be deleted.
     * @param onSuccess Callback invoked when the deletion is successful.
     * @param onError Callback invoked when an error occurs during deletion.
     */
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