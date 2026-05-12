package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.AdminDatastorePreference
import kotlinx.coroutines.flow.Flow

/**
 * Use case to determine if the battery optimization warning dialog should be shown.
 * Returns a [Flow] that emits true if the dialog should be shown, false otherwise.
 */
class GetShouldShowBatterieOptimizationUseCase(
    private val adminDatastorePreference: AdminDatastorePreference
) {
    /**
     * Executes the use case.
     * @return A Flow of Booleans.
     */
    operator fun invoke(): Flow<Boolean> {
        return adminDatastorePreference.shouldShowBatterieOptimizationFlow
    }

}