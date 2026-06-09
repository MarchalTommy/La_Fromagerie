package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.DeliveryPath

/**
 * Use case to add a new delivery path to the Firebase database.
 */
class AddNewPathUseCase(
    private val firebaseRepository: FirebaseAdminRepository
) {
    /**
     * Executes the use case.
     * @param path The new [DeliveryPath] to be added.
     * @param onSuccess Callback invoked when the path is successfully added.
     * @param onError Callback invoked when an error occurs during the operation.
     */
    suspend operator fun invoke(
        path: DeliveryPath,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        firebaseRepository.addNewDeliveryPath(path).onSuccess {
            onSuccess()
        }.onFailure {
            onError(it)
        }
    }
}