package com.mtdevelopment.lafromagerie.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mtdevelopment.lafromagerie.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

/**
 * Receives Firebase Cloud Messaging pushes for the client flavor.
 *
 * Called when the app is in foreground (any message) or in background for data-only
 * messages. Notification-messages received in background are posted by the FCM SDK itself
 * on the channel declared in the client manifest, without this callback.
 *
 * Targeting is topic-based ([TOPIC_ALL_CLIENTS], subscribed at startup in MainActivity):
 * no per-device token registry, so [onNewToken] has nothing to upload.
 */
class ClientMessagingService : FirebaseMessagingService(), KoinComponent {

    private val notificationStore: NotificationLocalStore by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data[DATA_KEY_TITLE]
        val body = message.notification?.body ?: message.data[DATA_KEY_BODY]
        if (title.isNullOrBlank() && body.isNullOrBlank()) {
            Log.w(TAG, "onMessageReceived: message without displayable content, ignored")
            return
        }

        val notification = InAppNotification(
            id = message.messageId ?: UUID.randomUUID().toString(),
            title = title.orEmpty().ifBlank { getString(R.string.app_name) },
            body = body.orEmpty(),
            timestampMillis = if (message.sentTime > 0) message.sentTime else System.currentTimeMillis()
        )

        serviceScope.launch {
            try {
                notificationStore.add(notification)
            } catch (e: Exception) {
                Log.e(TAG, "onMessageReceived: persist failed", e)
            }
        }

        ClientNotifier.post(this, notification)
    }

    override fun onNewToken(token: String) {
        // Topic-based targeting: nothing to register server-side. Never log the token
        // itself; it grants push access to this device.
        Log.i(TAG, "onNewToken: FCM token rotated")
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ClientMessagingService"
        const val TOPIC_ALL_CLIENTS = "clients"
        const val DATA_KEY_TITLE = "title"
        const val DATA_KEY_BODY = "body"
    }
}
