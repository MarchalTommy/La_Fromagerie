package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.AdminDatastorePreference
import kotlinx.coroutines.flow.Flow

/**
 * Use case to observe the current tracking mode status.
 * Returns a [Flow] that emits true if the app is in tracking mode, false otherwise.
 */
class GetIsInTrackingModeUseCase(
    private val adminDatastorePreference: AdminDatastorePreference
) {
    /**
     * Executes the use case.
     * @return A Flow of Booleans representing the tracking state.
     */
    operator fun invoke(): Flow<Boolean> {
        return adminDatastorePreference.isInTrackingModeFlow
    }
}