package com.mtdevelopment.checkout.domain.repository

import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentsClient
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

    fun createNewCheckout()

    fun processCheckout(id:String)
}