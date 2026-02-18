package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.PreparationStatus

class GetPreparationStatusesUseCase(
    private val firebaseAdminRepository: FirebaseAdminRepository
) {
    suspend operator fun invoke(onSuccess: (List<PreparationStatus>?) -> Unit) {
        firebaseAdminRepository.getPreparationStatuses(onSuccess)
    }
}
