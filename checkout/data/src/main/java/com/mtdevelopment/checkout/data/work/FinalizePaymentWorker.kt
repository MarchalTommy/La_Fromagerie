package com.mtdevelopment.checkout.data.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.CHECKOUT_STATUS
import com.mtdevelopment.checkout.data.remote.source.FirestoreOrderDataSource
import com.mtdevelopment.checkout.data.remote.source.SumUpDataSource
import com.mtdevelopment.checkout.data.work.FinalizePaymentWorker.Companion.MAX_RUN_ATTEMPTS
import com.mtdevelopment.checkout.domain.model.PendingPaymentFinalization
import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference
import com.mtdevelopment.core.model.OrderStatus
import com.mtdevelopment.core.repository.SharedDatastore
import com.mtdevelopment.core.util.NetWorkResult
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Durable safety net for the payment flow.
 *
 * Once a payment has been submitted to SumUp, the money side is out of our hands: the
 * in-app polling loop ([SumUpDataSource.pollCheckoutStatus]) normally waits for the
 * terminal status and updates the Firestore order. But if the app is killed in that
 * window, the customer may be charged while the order stays PENDING forever.
 *
 * This worker is enqueued (as unique, persisted work) right before the payment is
 * submitted. It polls SumUp for the terminal status and:
 * - on PAID: marks the Firestore order as PAID and clears the local cart,
 * - on FAILED: marks the Firestore order as CANCELED (the cart is kept so the
 *   customer can retry),
 * then clears the pending-finalization marker.
 *
 * Every step is idempotent, so it can safely race with the in-app flow: whichever
 * finishes first clears the marker and the other becomes a no-op.
 */
class FinalizePaymentWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val checkoutDatastorePreference: CheckoutDatastorePreference by inject()
    private val sumUpDataSource: SumUpDataSource by inject()
    private val firestoreOrderDataSource: FirestoreOrderDataSource by inject()
    private val sharedDatastore: SharedDatastore by inject()

    override suspend fun doWork(): Result {
        val pending = checkoutDatastorePreference.pendingFinalizationFlow.first()
        // No marker: the in-app flow already finalized this payment.
            ?: return Result.success()

        if (pending.isExpired(System.currentTimeMillis())) {
            // The SumUp session has expired; stop reconciling automatically.
            Log.w(TAG, "Pending finalization for ${pending.checkoutId} expired, giving up.")
            checkoutDatastorePreference.clearPendingFinalization()
            return Result.success()
        }

        Log.i(
            TAG,
            "Reconciling checkout ${pending.checkoutId} (attempt ${runAttemptCount + 1})"
        )

        // pollCheckoutStatus loops internally (up to ~2 minutes) and emits exactly once:
        // either a terminal status or an error/timeout.
        return when (val result = sumUpDataSource.pollCheckoutStatus(pending.checkoutId).first()) {
            is NetWorkResult.Success -> when (result.data.status) {
                CHECKOUT_STATUS.PAID -> finalize(pending, OrderStatus.PAID)
                CHECKOUT_STATUS.FAILED -> finalize(pending, OrderStatus.CANCELED)
                else -> retryOrGiveUp("Checkout still pending after polling")
            }

            is NetWorkResult.Error -> retryOrGiveUp(result.message)
        }
    }

    private suspend fun finalize(
        pending: PendingPaymentFinalization,
        status: OrderStatus
    ): Result {
        val updateResult = firestoreOrderDataSource.updateOrder(pending.orderId, status)
        if (updateResult.isFailure) {
            return retryOrGiveUp(
                "Failed to update order ${pending.orderId} to $status: " +
                        "${updateResult.exceptionOrNull()?.message}"
            )
        }

        if (status == OrderStatus.PAID) {
            sharedDatastore.clearCartItems()
        }
        checkoutDatastorePreference.clearPendingFinalization()
        Log.i(TAG, "Order ${pending.orderId} finalized as $status")
        return Result.success()
    }

    /**
     * Retries with WorkManager backoff until [MAX_RUN_ATTEMPTS]. Giving up keeps the
     * marker in place, so the work is re-enqueued at the next app launch (until the
     * marker itself expires).
     */
    private fun retryOrGiveUp(reason: String?): Result {
        return if (runAttemptCount < MAX_RUN_ATTEMPTS - 1) {
            Log.w(TAG, "Finalization not terminal yet ($reason), will retry.")
            Result.retry()
        } else {
            Log.e(TAG, "Finalization gave up after $MAX_RUN_ATTEMPTS attempts ($reason).")
            Result.failure()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "payment_finalization"
        private const val TAG = "FinalizePaymentWorker"
        private const val MAX_RUN_ATTEMPTS = 5
    }
}
