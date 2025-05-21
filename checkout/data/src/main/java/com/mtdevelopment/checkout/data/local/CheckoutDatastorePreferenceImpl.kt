package com.mtdevelopment.checkout.data.local

import android.content.Context
import androidx.annotation.Keep
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mtdevelopment.checkout.domain.model.NewCheckoutResult
import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderData
import com.mtdevelopment.core.model.toOrder
import com.mtdevelopment.core.model.toOrderData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Keep
class CheckoutDatastorePreferenceImpl(private val context: Context) : CheckoutDatastorePreference {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "checkout_settings")

    // BEARER TOKEN USED TO AUTH ON SUMUP API
    private val SUMUP_TOKEN = stringPreferencesKey("sumup_token")
    override val sumUpTokenFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SUMUP_TOKEN] ?: ""
    }

    override suspend fun setSumUpToken(token: String) {
        context.dataStore.edit { settings ->
            settings[SUMUP_TOKEN] = token
        }
    }

    // REFRESH TOKEN USED TO ASK A FRESH TOKEN TO SUMUP API
    private val SUMUP_REFRESH_TOKEN = stringPreferencesKey("sumup_refresh_token")
    override val sumUpRefreshTokenFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SUMUP_REFRESH_TOKEN] ?: ""
    }

    override suspend fun setSumUpRefreshToken(token: String) {
        context.dataStore.edit { settings ->
            settings[SUMUP_REFRESH_TOKEN] = token
        }
    }

    // VALIDITY OF THE TOKEN, TO KNOW WHEN TO REFRESH THE TOKEN FIRST
    private val SUMUP_TOKEN_VALIDITY = longPreferencesKey("sumup_token_validity")
    override val sumUpTokenValidityFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[SUMUP_TOKEN_VALIDITY] ?: 0L
    }

    override suspend fun setSumUpTokenValidity(duration: Long) {
        context.dataStore.edit { settings ->
            settings[SUMUP_TOKEN_VALIDITY] = duration
        }
    }

    private val CHECKOUT_REFERENCE = stringSetPreferencesKey("checkout_reference")
    override val checkoutReferencesFlow: Flow<List<String>> =
        context.dataStore.data.map { preferences ->
            preferences[CHECKOUT_REFERENCE]?.toList() ?: setOf<String>().toList()
        }

    override suspend fun saveCheckoutReference(reference: String) {
        context.dataStore.edit { settings ->
            if (settings[CHECKOUT_REFERENCE] != null) {
                val newSet = settings[CHECKOUT_REFERENCE]?.toMutableSet()
                newSet?.add(reference)
                newSet?.let {
                    settings[CHECKOUT_REFERENCE] = newSet.toSet()
                } ?: run {
                    settings[CHECKOUT_REFERENCE] = setOf(reference)
                }
            } else {
                settings[CHECKOUT_REFERENCE] = setOf(reference)
            }
        }
    }

    private val CREATED_CHECKOUT = stringPreferencesKey("created_checkout")
    override val createdCheckoutFlow: Flow<NewCheckoutResult> =
        context.dataStore.data.map { preferences ->
            Json.decodeFromString(preferences[CREATED_CHECKOUT] ?: "")
        }

    override suspend fun saveCreatedCheckout(data: NewCheckoutResult) {
        context.dataStore.edit { settings ->
            settings[CREATED_CHECKOUT] = Json.encodeToString(data)
        }
    }

    private val IS_CHECKOUT_SUCCESS = booleanPreferencesKey("IS_CHECKOUT_SUCCESS")
    override val isCheckoutSuccessfulFlow: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[IS_CHECKOUT_SUCCESS] ?: false
        }

    override suspend fun setIsCheckoutSuccessful(isSuccess: Boolean) {
        context.dataStore.edit { settings ->
            settings[IS_CHECKOUT_SUCCESS] = isSuccess
        }
    }

    override suspend fun resetCheckoutStatus() {
        context.dataStore.edit {
            it.remove(IS_CHECKOUT_SUCCESS)
        }
    }

    private val ORDER_ITEM = stringPreferencesKey("ORDER_ITEM")
    override val orderFlow: Flow<Order?>
        get() = context.dataStore.data.map { preferences ->
            (Json.decodeFromString(preferences[ORDER_ITEM] ?: "") as OrderData?)?.toOrder()
        }


    override suspend fun saveOrder(order: Order) {
        context.dataStore.edit { settings ->
            settings[ORDER_ITEM] = Json.encodeToString(order.toOrderData())
        }
    }
}