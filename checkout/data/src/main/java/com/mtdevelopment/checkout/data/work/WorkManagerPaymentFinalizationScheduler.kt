package com.mtdevelopment.checkout.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mtdevelopment.checkout.domain.repository.PaymentFinalizationScheduler
import java.util.concurrent.TimeUnit

/**
 * WorkManager-backed implementation: the enqueued work is persisted by the system,
 * so it survives the app being killed and even device reboots.
 */
class WorkManagerPaymentFinalizationScheduler(
    private val context: Context
) : PaymentFinalizationScheduler {

    override fun scheduleFinalizationWork() {
        val request = OneTimeWorkRequestBuilder<FinalizePaymentWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .build()

        // REPLACE: a new payment (or an app-launch resume) always restarts the
        // reconciliation from a clean attempt counter.
        WorkManager.getInstance(context).enqueueUniqueWork(
            FinalizePaymentWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
