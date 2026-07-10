package com.mtdevelopment.checkout.data.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.CHECKOUT_STATUS
import com.mtdevelopment.checkout.data.remote.source.FirestoreOrderDataSource
import com.mtdevelopment.checkout.data.remote.source.SumUpDataSource
import com.mtdevelopment.checkout.data.work.FinalizePaymentWorker.Companion.MAX_RUN_ATTEMPTS
import com.mtdevelopment.checkout.domain.model.PaymentOutcome
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
 * submitted (Google Pay path) or right before the hosted-checkout page is opened
 * (hosted path, where the customer may pay in the browser and never return to the
 * app). It polls SumUp for the terminal status and:
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
            Log.w(TAG, "Pending finalization for order ${pending.orderId} expired, giving up.")
            checkoutDatastorePreference.clearPendingFinalization()
            return Result.success()
        }

        Log.i(
            TAG,
            "Reconciling order ${pending.orderId} " +
                    "(checkoutId=${pending.checkoutId}, attempt ${runAttemptCount + 1})"
        )

        // Both polling calls loop internally (up to ~2 minutes) and emit exactly once:
        // either a terminal status or an error/timeout. The hosted-checkout path never
        // learns the SumUp session id, so its marker is reconciled through the
        // checkout_reference (= the order id) with an amount-integrity check.
        val checkoutId = pending.checkoutId
        val result = if (checkoutId != null) {
            sumUpDataSource.pollCheckoutStatus(checkoutId).first()
        } else {
            sumUpDataSource
                .pollHostedCheckoutStatus(pending.orderId, pending.expectedAmountCents)
                .first()
        }
        return when (result) {
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

        // Reaching here means the in-app flow was gone (app killed / backgrounded),
        // otherwise it would have cleared the marker and this worker would have no-oped.
        // Record the outcome so the app can tell the customer on its next launch — on
        // PAID it routes to the success screen, otherwise it shows a "cart kept" notice.
        val clientName = checkoutDatastorePreference.orderFlow.first()
            ?.takeIf { it.id == pending.orderId }
            ?.customerName
            .orEmpty()
        checkoutDatastorePreference.setPaymentOutcome(
            PaymentOutcome(
                orderId = pending.orderId,
                clientName = clientName,
                isPaid = status == OrderStatus.PAID
            )
        )

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
