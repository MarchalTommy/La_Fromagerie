package com.mtdevelopment.checkout.domain.repository

import com.google.android.gms.wallet.PaymentsClient
import com.mtdevelopment.checkout.domain.model.Checkout
import com.mtdevelopment.checkout.domain.model.GooglePayData
import com.mtdevelopment.checkout.domain.model.NewCheckoutResult
import com.mtdevelopment.checkout.domain.model.ProcessCheckoutResult
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

interface PaymentRepository {

    ///////////////////////////////////////////////////////////////////////////
    // GOOGLE PAY
    ///////////////////////////////////////////////////////////////////////////
    fun createPaymentsClient(): PaymentsClient

    val allowedPaymentMethods: JSONArray

    fun isReadyToPayRequest(): JSONObject?

    suspend fun canUseGooglePay(): Boolean?

    fun getPaymentDataRequest(priceCents: Long): JSONObject

    ///////////////////////////////////////////////////////////////////////////
    // SUMUP
    ///////////////////////////////////////////////////////////////////////////

    fun createNewCheckout(
        amount: Double,
        description: String,
        buyerName: String,
        buyerAddress: String,
        buyerEmail: String,
        reference: String
    ): Flow<NewCheckoutResult>

    fun processCheckout(
        checkoutId: String,
        googlePayData: GooglePayData,
        on3DSecureRequired: (ProcessCheckoutResult.NextStep) -> Unit
    ): Flow<Checkout>

    suspend fun getSumUpPaymentLink(amount: Double, orderId: String): Result<String>

    /**
     * Resiliently polls the hosted-checkout outcome by its reference (= orderId), with an
     * amount-integrity check, for the foreground "customer just returned from SumUp" path.
     *
     * Emits exactly once:
     * - [Result.success] with the terminal [Checkout] — either PAID (amount matches
     *   [expectedAmountCents]) or FAILED;
     * - [Result.failure] ([HostedCheckoutStatusUnresolvedException]) when the outcome is
     *   still unknown within the interactive budget (session still processing, or a
     *   transient connectivity blip). Callers MUST NOT tell the customer they were not
     *   charged in this case: the durable finalization worker keeps reconciling.
     */
    fun pollHostedCheckoutStatus(
        reference: String,
        expectedAmountCents: Long?
    ): Flow<Result<Checkout>>

    ///////////////////////////////////////////////////////////////////////////
    // ORDERS
    ///////////////////////////////////////////////////////////////////////////
    suspend fun createFirestoreOrder(order: Order): Result<Unit>

    suspend fun updateFirestoreOrderStatus(orderId: String, newStatus: OrderStatus): Result<Unit>
}

/**
 * Raised when [PaymentRepository.pollHostedCheckoutStatus] cannot resolve a terminal
 * outcome within its budget (still processing, or transient connectivity). Signals
 * "unknown", never "not charged".
 */
class HostedCheckoutStatusUnresolvedException(message: String?) : Exception(message)