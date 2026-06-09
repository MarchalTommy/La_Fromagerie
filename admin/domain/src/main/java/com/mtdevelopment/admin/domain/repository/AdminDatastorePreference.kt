package com.mtdevelopment.admin.domain.repository

import com.mtdevelopment.admin.domain.model.OptimizedRouteWithOrders
import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing administrative preferences and persistent state using DataStore.
 * This includes delivery path caching, UI settings like battery optimization dialogs, and tracking state.
 */
interface AdminDatastorePreference {

    /**
     * Flow of the geocoded and optimized daily delivery path.
     * Returns null if no delivery path is currently set.
     */
    val dailyDeliveryPathGeocodedFlow: Flow<OptimizedRouteWithOrders?>

    /**
     * Saves the geocoded and optimized daily delivery path to persistent storage.
     * @param delivery The optimized route to save.
     */
    suspend fun setDailyDeliveryPathGeocoded(delivery: OptimizedRouteWithOrders)

    /**
     * Flow indicating whether the battery optimization dialog should be shown to the user.
     */
    val shouldShowBatterieOptimizationFlow: Flow<Boolean>

    /**
     * Updates the preference for showing the battery optimization dialog.
     * @param shouldShow True if the dialog should be shown, false otherwise.
     */
    suspend fun updateShouldShowBatterieOptimization(shouldShow: Boolean)

    /**
     * Flow indicating whether the application is currently in tracking mode (e.g., during delivery).
     */
    val isInTrackingModeFlow: Flow<Boolean>

    /**
     * Updates the tracking mode state.
     * @param isInTrackingMode True if tracking is enabled, false otherwise.
     */
    suspend fun setIsInTrackingMode(isInTrackingMode: Boolean)

    /**
     * Resets the daily delivery path data.
     */
    suspend fun resetDailyDelivery()
}