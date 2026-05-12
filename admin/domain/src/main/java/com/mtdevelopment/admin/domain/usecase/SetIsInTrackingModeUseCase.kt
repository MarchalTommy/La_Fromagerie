package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.AdminDatastorePreference

/**
 * Use case to enable or disable the tracking mode.
 */
class SetIsInTrackingModeUseCase(
    private val adminDatastorePreference: AdminDatastorePreference
) {
    /**
     * Executes the use case.
     * @param isInTrackingMode True to enable tracking, false to disable.
     */
    suspend operator fun invoke(isInTrackingMode: Boolean) {
        adminDatastorePreference.setIsInTrackingMode(isInTrackingMode)
    }
}