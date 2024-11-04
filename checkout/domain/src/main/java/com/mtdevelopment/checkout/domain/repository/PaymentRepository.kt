package com.mtdevelopment.checkout.domain.repository

import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentsClient
import com.mtdevelopment.checkout.domain.model.GooglePayData
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

    suspend fun getCheckoutFromRef(reference: String?)

    suspend fun getCheckoutFromId(id: String)

    fun createNewCheckout(amount: Double, reference: String): Flow<Any>

    fun processCheckout(
        reference: String,
        googlePayData: GooglePayData.PaymentMethodData
    ): Flow<Any>
}