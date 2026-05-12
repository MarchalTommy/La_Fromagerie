package com.mtdevelopment.admin.data.repository

import android.content.Context
import androidx.annotation.Keep
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mtdevelopment.admin.domain.model.OptimizedRouteWithOrders
import com.mtdevelopment.admin.domain.repository.AdminDatastorePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Implementation of [AdminDatastorePreference] using Jetpack DataStore.
 * This class stores administrative preferences and caches the daily optimized delivery route.
 * Complex objects like [OptimizedRouteWithOrders] are serialized to JSON strings for storage.
 */
@Keep
class AdminDatastorePreferenceImpl(private val context: Context) : AdminDatastorePreference {

    /**
     * DataStore instance for 'admin_data' preferences.
     */
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "admin_data")

    /**
     * Key for the geocoded daily delivery path.
     */
    private val dailyDeliveryPathGeocodedKey = stringPreferencesKey("daily_delivery_path_geocoded")

    /**
     * Flow that provides the cached daily delivery path.
     * It attempts to deserialize the JSON string into an [OptimizedRouteWithOrders] object.
     */
    override val dailyDeliveryPathGeocodedFlow: Flow<OptimizedRouteWithOrders?> =
        context.dataStore.data.map { preferences ->
            try {
                (Json.decodeFromString(
                    preferences[dailyDeliveryPathGeocodedKey] ?: ""
                ) as OptimizedRouteWithOrders)
            } catch (e: Exception) {
                // Returns null if deserialization fails or if key is missing
                null
            }
        }

    /**
     * Serializes and saves the daily delivery path to DataStore.
     */
    override suspend fun setDailyDeliveryPathGeocoded(delivery: OptimizedRouteWithOrders) {
        context.dataStore.edit { settings ->
            settings[dailyDeliveryPathGeocodedKey] = Json.encodeToString(delivery)
        }
    }

    /**
     * Key for showing battery optimization dialog.
     */
    private val shouldShowBatterieOptimizationKey = booleanPreferencesKey("batterie_optimization")

    /**
     * Flow of the battery optimization preference.
     */
    override val shouldShowBatterieOptimizationFlow: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[shouldShowBatterieOptimizationKey] == true
        }

    /**
     * Updates the battery optimization preference.
     */
    override suspend fun updateShouldShowBatterieOptimization(shouldShow: Boolean) {
        context.dataStore.edit { settings ->
            settings[shouldShowBatterieOptimizationKey] = shouldShow
        }
    }

    /**
     * Key for tracking mode status.
     */
    private val isInTrackingModeKey = booleanPreferencesKey("is_in_tracking_mode")

    /**
     * Flow of the current tracking mode state.
     */
    override val isInTrackingModeFlow: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[isInTrackingModeKey] == true
        }

    /**
     * Updates the tracking mode state in DataStore.
     */
    override suspend fun setIsInTrackingMode(isInTrackingMode: Boolean) {
        context.dataStore.edit { settings ->
            settings[isInTrackingModeKey] = isInTrackingMode
        }
    }

    /**
     * Clears the cached daily delivery path.
     */
    override suspend fun resetDailyDelivery() {
        context.dataStore.edit {
            it.remove(dailyDeliveryPathGeocodedKey)
        }
    }

}