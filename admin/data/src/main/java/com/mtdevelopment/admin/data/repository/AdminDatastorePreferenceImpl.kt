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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Keep
class AdminDatastorePreferenceImpl(private val context: Context) : AdminDatastorePreference {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "admin_data")

    private val dailyDeliveryPathGeocodedKey = stringPreferencesKey("daily_delivery_path_geocoded")

    override val dailyDeliveryPathGeocodedFlow: Flow<OptimizedRouteWithOrders?> =
        context.dataStore.data.map { preferences ->
            try {
                (Json.decodeFromString(
                    preferences[dailyDeliveryPathGeocodedKey] ?: ""
                ) as OptimizedRouteWithOrders)
            } catch (e: Exception) {
                null
            }
        }

    override suspend fun setDailyDeliveryPathGeocoded(delivery: OptimizedRouteWithOrders) {
        context.dataStore.edit { settings ->
            settings[dailyDeliveryPathGeocodedKey] = Json.encodeToString(delivery)
        }
    }

    private val shouldShowBatterieOptimizationKey = booleanPreferencesKey("batterie_optimization")

    override val shouldShowBatterieOptimizationFlow: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[shouldShowBatterieOptimizationKey] == true
        }

    override suspend fun updateShouldShowBatterieOptimization(shouldShow: Boolean) {
        context.dataStore.edit { settings ->
            settings[shouldShowBatterieOptimizationKey] = shouldShow
        }
    }

    override suspend fun resetDailyDelivery() {
        context.dataStore.edit {
            it.remove(dailyDeliveryPathGeocodedKey)
        }
    }

}