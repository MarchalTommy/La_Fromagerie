package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.AdminDatastorePreference

class SetIsInTrackingModeUseCase(
    private val adminDatastorePreference: AdminDatastorePreference
) {
    suspend operator fun invoke(isInTrackingMode: Boolean) {
        adminDatastorePreference.setIsInTrackingMode(isInTrackingMode)
    }
}