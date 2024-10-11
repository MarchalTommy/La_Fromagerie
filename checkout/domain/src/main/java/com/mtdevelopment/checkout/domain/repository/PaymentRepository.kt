package com.mtdevelopment.checkout.domain.repository

import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentsClient
import org.json.JSONObject

interface PaymentRepository {

    ///////////////////////////////////////////////////////////////////////////
    // GOOGLE PAY
    ///////////////////////////////////////////////////////////////////////////

    val baseRequest: JSONObject

    val cardPaymentMethod: JSONObject

    fun createPaymentsClient(): PaymentsClient?

    suspend fun fetchCanUseGooglePay(): Boolean?

    fun getLoadPaymentDataTask(
        price: String
    ): Task<PaymentData>?

    ///////////////////////////////////////////////////////////////////////////
    // SUMUP
    ///////////////////////////////////////////////////////////////////////////

}