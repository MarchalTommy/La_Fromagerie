package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.PreparationStatus

/**
 * Use case to update an existing preparation status in the Firebase database.
 */
class UpdatePreparationStatusUseCase(
    private val firebaseAdminRepository: FirebaseAdminRepository
) {
    /**
     * Executes the use case.
     * @param status The [PreparationStatus] to update.
     * @return [Result] indicating the outcome of the operation.
     */
    suspend operator fun invoke(status: PreparationStatus): Result<Unit> {
        return firebaseAdminRepository.updatePreparationStatus(status)
    }
}
