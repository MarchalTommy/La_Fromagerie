package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.AdminDatastorePreference

/**
 * Use case to update the preference for showing the battery optimization warning dialog.
 */
class UpdateShouldShowBatterieOptimizationUseCase(
    private val adminDatastorePreference: AdminDatastorePreference
) {
    /**
     * Executes the use case.
     * @param shouldShow True if the dialog should be shown in the future, false otherwise.
     */
    suspend operator fun invoke(shouldShow: Boolean) {
        adminDatastorePreference.updateShouldShowBatterieOptimization(shouldShow)
    }
}