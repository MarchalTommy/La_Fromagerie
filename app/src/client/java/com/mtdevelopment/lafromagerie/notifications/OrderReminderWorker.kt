package com.mtdevelopment.lafromagerie.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mtdevelopment.core.model.FulfillmentType
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
 * The wording follows the order's fulfillment mode, which rides in the input data rather
 * than being read back: the saved order is cleared when the purchase completes, long before
 * the morning this fires. Promising a delivery to a customer who is coming to collect would
 * be wrong, and it would omit the two things they need — the point and its opening window.
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

        val fulfillmentType = FulfillmentType.fromStoredValue(
            inputData.getString(KEY_FULFILLMENT_TYPE)
        )

        val notification = InAppNotification(
            // Derived from the order id rather than random: NotificationLocalStore.add
            // ignores an id it already holds, so a reminder that somehow fires twice for
            // one order leaves a single entry in the notification center.
            id = "$NOTIFICATION_ID_PREFIX$orderId",
            title = titleFor(fulfillmentType),
            body = bodyFor(
                fulfillmentType = fulfillmentType,
                pickupLabel = inputData.getString(KEY_PICKUP_LABEL),
                pickupTimeRange = inputData.getString(KEY_PICKUP_TIME_RANGE)
            ),
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
        const val KEY_FULFILLMENT_TYPE = "fulfillment_type"
        const val KEY_PICKUP_LABEL = "pickup_label"
        const val KEY_PICKUP_TIME_RANGE = "pickup_time_range"
        const val UNIQUE_WORK_PREFIX = "order_reminder_"
        private const val NOTIFICATION_ID_PREFIX = "reminder_"
        private const val TAG = "OrderReminderWorker"

        private const val DELIVERY_TITLE = "Votre commande arrive aujourd'hui"
        private const val PICKUP_TITLE = "Votre commande est à retirer aujourd'hui"
        private const val DELIVERY_BODY =
            "Votre commande La Fromagerie est prévue pour aujourd'hui. " +
                    "Pensez à rester joignable pour la livraison !"
        private const val PICKUP_BODY_WITHOUT_POINT =
            "Votre commande La Fromagerie est prête pour aujourd'hui."

        /** Nothing is promised about a delivery to someone who is coming to collect. */
        fun titleFor(fulfillmentType: FulfillmentType): String =
            if (fulfillmentType.isPickup) PICKUP_TITLE else DELIVERY_TITLE

        /**
         * The reminder a collecting customer actually needs: where, and until when.
         *
         * Falls back to wording that mentions neither a delivery nor a place when the
         * snapshot is missing — an order placed by an app version that did not carry these
         * fields must still get a reminder that is merely vague, never wrong.
         */
        fun bodyFor(
            fulfillmentType: FulfillmentType,
            pickupLabel: String?,
            pickupTimeRange: String?
        ): String {
            if (!fulfillmentType.isPickup) return DELIVERY_BODY

            val label = pickupLabel?.trim().orEmpty()
            if (label.isBlank()) return PICKUP_BODY_WITHOUT_POINT

            val timeRange = pickupTimeRange?.trim().orEmpty()
            val place = if (timeRange.isBlank()) label else "$label, $timeRange"
            return "Votre commande La Fromagerie est à retirer aujourd'hui — $place."
        }
    }
}
