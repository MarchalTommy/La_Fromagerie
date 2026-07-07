package com.mtdevelopment.lafromagerie.notifications

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Local persistence of the client notification center, backed by its own DataStore file.
 *
 * Must stay a Koin single: two DataStore instances on the same file crash at runtime.
 * Written from [ClientMessagingService] (possibly with no activity alive) and read from
 * [NotificationViewModel].
 */
class NotificationLocalStore(private val context: Context, private val json: Json) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "client_notifications")

    private val listSerializer = ListSerializer(InAppNotification.serializer())

    val notificationsFlow: Flow<List<InAppNotification>>
        get() = context.dataStore.data.map { preferences ->
            decode(preferences[NOTIFICATIONS_KEY])
        }

    /** Inserts newest-first; ignores duplicates (same FCM message id) and caps the history. */
    suspend fun add(notification: InAppNotification) {
        context.dataStore.edit { preferences ->
            val current = decode(preferences[NOTIFICATIONS_KEY])
            if (current.none { it.id == notification.id }) {
                preferences[NOTIFICATIONS_KEY] = json.encodeToString(
                    listSerializer,
                    (listOf(notification) + current).take(MAX_HISTORY)
                )
            }
        }
    }

    suspend fun markAllRead() {
        context.dataStore.edit { preferences ->
            val current = decode(preferences[NOTIFICATIONS_KEY])
            if (current.any { !it.isRead }) {
                preferences[NOTIFICATIONS_KEY] = json.encodeToString(
                    listSerializer,
                    current.map { it.copy(isRead = true) }
                )
            }
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.remove(NOTIFICATIONS_KEY) }
    }

    private fun decode(raw: String?): List<InAppNotification> =
        if (raw.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                json.decodeFromString(listSerializer, raw)
            } catch (e: Exception) {
                Log.e(TAG, "decode:", e)
                emptyList()
            }
        }

    companion object {
        private const val TAG = "NotificationLocalStore"
        private val NOTIFICATIONS_KEY = stringPreferencesKey("notifications")
        private const val MAX_HISTORY = 50
    }
}
