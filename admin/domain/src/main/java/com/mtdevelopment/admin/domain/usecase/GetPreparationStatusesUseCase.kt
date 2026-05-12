package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.PreparationStatus

/**
 * Use case to retrieve all possible preparation statuses from the Firebase database.
 */
class GetPreparationStatusesUseCase(
    private val firebaseAdminRepository: FirebaseAdminRepository
) {
    /**
     * Executes the use case.
     * @param onSuccess Callback invoked with the list of preparation statuses, or null if an error occurs.
     */
    suspend operator fun invoke(onSuccess: (List<PreparationStatus>?) -> Unit) {
        firebaseAdminRepository.getPreparationStatuses(onSuccess)
    }
}
