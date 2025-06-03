package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.AdminDatastorePreference
import kotlinx.coroutines.flow.Flow

class GetIsInTrackingModeUseCase(
    private val adminDatastorePreference: AdminDatastorePreference
) {
    operator fun invoke(): Flow<Boolean> {
        return adminDatastorePreference.isInTrackingModeFlow
    }
}