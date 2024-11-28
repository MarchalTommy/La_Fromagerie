package com.mtdevelopment.core.local

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.model.CartItemsData
import com.mtdevelopment.core.model.UserInformation
import com.mtdevelopment.core.model.UserInformationData
import com.mtdevelopment.core.model.toCartItems
import com.mtdevelopment.core.model.toCartItemsData
import com.mtdevelopment.core.model.toUserInformation
import com.mtdevelopment.core.model.toUserInformationData
import com.mtdevelopment.core.repository.SharedDatastore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Keep
class SharedDatastoreImpl(private val context: Context) : SharedDatastore {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shared_settings")
    private val gson = Gson()

    // User Cart items
    private val CART_ITEMS_KEY = stringPreferencesKey("cart_items")
    override val cartItemsFlow: Flow<CartItems?>
        get() = context.dataStore.data.map { preferences ->
            try {
                gson.fromJson(preferences[CART_ITEMS_KEY], CartItemsData::class.java).toCartItems()
            } catch (e: Exception) {
                Log.e(TAG, "cartItemsFlow:", e)
                null
            }
        }

    override suspend fun setCartItems(cartItems: CartItems) {
        context.dataStore.edit {
            try {
                it[CART_ITEMS_KEY] = gson.toJson(cartItems.toCartItemsData())
            } catch (e: Exception) {
                Log.e(TAG, "setCartItems:", e)
            }
        }
    }

    override suspend fun clearCartItems() {
        context.dataStore.edit {
            it.remove(CART_ITEMS_KEY)
        }
    }

    // User information
    private val USER_INFORMATION_KEY = stringPreferencesKey("user_information")
    override val userInformationFlow: Flow<UserInformation?>
        get() = context.dataStore.data.map { preferences ->
            try {
                gson.fromJson(preferences[USER_INFORMATION_KEY], UserInformationData::class.java)
                    .toUserInformation()
            } catch (e: Exception) {
                Log.e(TAG, "userInformationFlow:", e)
                null
            }
        }

    override suspend fun setUserInformation(userInformation: UserInformation) {
        context.dataStore.edit {
            try {
                it[USER_INFORMATION_KEY] = gson.toJson(userInformation.toUserInformationData())
            } catch (e: Exception) {
                Log.e(TAG, "setUserInformation:", e)
            }
        }
    }

    override suspend fun clearUserInformation() {
        context.dataStore.edit {
            it.remove(USER_INFORMATION_KEY)
        }
    }

    // Delivery date
    private val DELIVERY_DATE_KEY = longPreferencesKey("delivery_date")
    override val deliveryDateFlow: Flow<Long>
        get() = context.dataStore.data.map { preferences ->
            preferences[DELIVERY_DATE_KEY] ?: 0L
        }

    override suspend fun setDeliveryDate(date: Long) {
        context.dataStore.edit {
            it[DELIVERY_DATE_KEY] = date
        }
    }

    override suspend fun clearDeliveryDate() {
        context.dataStore.edit {
            it.remove(DELIVERY_DATE_KEY)
        }
    }


    // Clearing data
    override suspend fun clearOrder() {
        context.dataStore.edit {
            it.remove(CART_ITEMS_KEY)
            it.remove(DELIVERY_DATE_KEY)
        }
    }

    private val LAST_FIRESTORE_PRODUCT_UPDATE = longPreferencesKey("firestore_product_update")
    override val lastFirestoreProductsUpdate: Flow<Long>
        get() = context.dataStore.data.map {
            it[LAST_FIRESTORE_PRODUCT_UPDATE] ?: 0L
        }

    override suspend fun lastFirestoreProductsUpdate(timestamp: Long) {
        context.dataStore.edit {
            it[LAST_FIRESTORE_PRODUCT_UPDATE] = timestamp
        }
    }

    private val LAST_FIRESTORE_PATH_UPDATE = longPreferencesKey("firestore_path_update")
    override val lastFirestorePathsUpdate: Flow<Long>
        get() = context.dataStore.data.map {
            it[LAST_FIRESTORE_PATH_UPDATE] ?: 0L
        }

    override suspend fun lastFirestorePathsUpdate(timestamp: Long) {
        context.dataStore.edit {
            it[LAST_FIRESTORE_PATH_UPDATE] = timestamp
        }
    }

    private val SHOULD_REFRESH_PRODUCTS = booleanPreferencesKey("should_refresh_products")
    override val shouldRefreshProducts: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[SHOULD_REFRESH_PRODUCTS] ?: true
        }

    override suspend fun setShouldRefreshProducts(shouldRefresh: Boolean) {
        context.dataStore.edit {
            it[SHOULD_REFRESH_PRODUCTS] = shouldRefresh
        }
    }

    private val SHOULD_REFRESH_PATHS = booleanPreferencesKey("should_refresh_paths")
    override val shouldRefreshPaths: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[SHOULD_REFRESH_PATHS] ?: true
        }

    override suspend fun setShouldRefreshPaths(shouldRefresh: Boolean) {
        context.dataStore.edit {
            it[SHOULD_REFRESH_PATHS] = shouldRefresh
        }
    }

    override suspend fun clearAllDatastore() {
        context.dataStore.edit {
            it.clear()
        }
    }
}