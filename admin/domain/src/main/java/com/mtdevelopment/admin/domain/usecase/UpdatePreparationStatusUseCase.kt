package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.PreparationStatus

class UpdatePreparationStatusUseCase(
    private val firebaseAdminRepository: FirebaseAdminRepository
) {
    suspend operator fun invoke(status: PreparationStatus): Result<Unit> {
        return firebaseAdminRepository.updatePreparationStatus(status)
    }
}
