package com.mtdevelopment.lafromagerie.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mtdevelopment.lafromagerie.MainActivity
import com.mtdevelopment.lafromagerie.R

/**
 * Single place where the client flavor posts a system notification.
 *
 * Shared by the two producers — pushes received through [ClientMessagingService] and
 * local order reminders raised by [OrderReminderWorker] — so both land on the same
 * channel, with the same icon and the same tap target. Splitting them would let the two
 * drift, and the channel id is also declared in the client manifest as FCM's default.
 */
internal object ClientNotifier {

    const val CHANNEL_ID = "fromagerie_client_general"
    private const val CHANNEL_NAME = "Actualités et rappels"
    private const val TAG = "ClientNotifier"

    /**
     * Posts [notification] to the system tray. Silently skipped when the user has
     * revoked notifications: the refusal is respected and the entry still lives in the
     * in-app notification center.
     */
    fun post(context: Context, notification: InAppNotification) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) {
            Log.i(TAG, "post: notifications disabled by user, in-app only")
            return
        }

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val systemNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.appicon_simpler)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        try {
            manager.notify(notification.id.hashCode(), systemNotification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS revoked between the check and the call.
            Log.w(TAG, "post:", e)
        }
    }
}
