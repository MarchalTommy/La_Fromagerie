package com.mtdevelopment.lafromagerie.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Raises the reminder for one order on the morning it is due.
 *
 * Enqueued at payment time by [WorkManagerOrderReminderScheduler] and persisted by
 * WorkManager, so it survives the app being killed and the device rebooting — which an
 * `AlarmManager` would only match at the cost of the exact-alarm permission introduced in
 * Android 12.
 *
 * The entry is written to the in-app notification center **before** the system
 * notification is posted: the tray copy can be swiped away or suppressed outright when
 * the user declined POST_NOTIFICATIONS, and the notification center is then the only
 * remaining trace.
 */
class OrderReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val notificationStore: NotificationLocalStore by inject()

    override suspend fun doWork(): Result {
        val orderId = inputData.getString(KEY_ORDER_ID)
        if (orderId.isNullOrBlank()) {
            Log.w(TAG, "doWork: no order id in input, nothing to remind about")
            return Result.failure()
        }

        val notification = InAppNotification(
            // Derived from the order id rather than random: NotificationLocalStore.add
            // ignores an id it already holds, so a reminder that somehow fires twice for
            // one order leaves a single entry in the notification center.
            id = "$NOTIFICATION_ID_PREFIX$orderId",
            title = TITLE,
            body = BODY,
            timestampMillis = System.currentTimeMillis()
        )

        try {
            notificationStore.add(notification)
        } catch (e: Exception) {
            // The tray notification is still worth posting: a persistence failure must
            // not swallow the reminder itself.
            Log.e(TAG, "doWork: persisting reminder for $orderId failed", e)
        }

        ClientNotifier.post(applicationContext, notification)
        Log.i(TAG, "doWork: reminder raised for order $orderId")
        return Result.success()
    }

    companion object {
        const val KEY_ORDER_ID = "order_id"
        const val UNIQUE_WORK_PREFIX = "order_reminder_"
        private const val NOTIFICATION_ID_PREFIX = "reminder_"
        private const val TAG = "OrderReminderWorker"
        private const val TITLE = "Votre commande arrive aujourd'hui"
        private const val BODY =
            "Votre commande La Fromagerie est prévue pour aujourd'hui. " +
                    "Pensez à rester joignable pour la livraison !"
    }
}
