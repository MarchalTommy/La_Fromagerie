package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.AdminDatastorePreference

class UpdateShouldShowBatterieOptimizationUseCase(
    private val adminDatastorePreference: AdminDatastorePreference
) {
    suspend operator fun invoke(shouldShow: Boolean) {
        adminDatastorePreference.updateShouldShowBatterieOptimization(shouldShow)
    }
}