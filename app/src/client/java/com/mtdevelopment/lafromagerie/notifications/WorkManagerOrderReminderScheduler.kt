package com.mtdevelopment.lafromagerie.notifications

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mtdevelopment.checkout.domain.model.OrderReminder
import com.mtdevelopment.checkout.domain.repository.OrderReminderScheduler
import java.util.concurrent.TimeUnit

/**
 * Client-flavor implementation: a persisted WorkManager job per order, fired on the
 * morning of the delivery day.
 *
 * No network constraint — the reminder carries no server data, so it must fire in a cellar
 * with no signal just as well as anywhere else. WorkManager honours the initial delay but
 * not to the minute (Doze can defer it); that is deliberate and sufficient for a "today"
 * reminder, and it avoids the exact-alarm permission an `AlarmManager` would demand.
 */
class WorkManagerOrderReminderScheduler(
    private val context: Context
) : OrderReminderScheduler {

    override fun scheduleReminder(orderId: String, deliveryDate: String) {
        val reminderMillis = OrderReminder.reminderTimeMillis(
            deliveryDate = deliveryDate,
            nowMillis = System.currentTimeMillis()
        )
        if (reminderMillis == null) {
            // Unparseable date, or a slot already past: staying silent beats reminding
            // someone about a day that is over.
            Log.i(TAG, "scheduleReminder: nothing to schedule for $orderId ($deliveryDate)")
            return
        }

        val request = OneTimeWorkRequestBuilder<OrderReminderWorker>()
            .setInitialDelay(reminderMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(OrderReminderWorker.KEY_ORDER_ID to orderId))
            .build()

        // REPLACE keyed on the order: re-finalizing the same payment (in-app flow racing
        // the durable worker) must not produce two reminders.
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(orderId),
            ExistingWorkPolicy.REPLACE,
            request
        )
        Log.i(TAG, "scheduleReminder: order $orderId due $deliveryDate")
    }

    override fun cancelReminder(orderId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(orderId))
        Log.i(TAG, "cancelReminder: order $orderId")
    }

    private fun uniqueWorkName(orderId: String) =
        "${OrderReminderWorker.UNIQUE_WORK_PREFIX}$orderId"

    private companion object {
        const val TAG = "OrderReminderScheduler"
    }
}
