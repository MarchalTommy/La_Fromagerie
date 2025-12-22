package com.mtdevelopment.checkout.data.remote.model.response.sumUp


import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class ProcessCheckoutResponseNew(
    @SerialName("amount")
    val amount: Double? = null,
    @SerialName("checkout_reference")
    val checkoutReference: String? = null,
    @SerialName("currency")
    val currency: String? = null,
    @SerialName("customer_id")
    val customerId: String? = null,
    @SerialName("date")
    val date: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("id")
    val id: String? = null,
    @SerialName("mandate")
    val mandate: Mandate? = null,
    @SerialName("merchant_code")
    val merchantCode: String? = null,
    @SerialName("return_url")
    val returnUrl: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("transaction_code")
    val transactionCode: String? = null,
    @SerialName("transaction_id")
    val transactionId: String? = null,
    @SerialName("transactions")
    val transactions: List<Transaction?>? = null,
    @SerialName("valid_until")
    val validUntil: String? = null
) {
    @Keep
    @Serializable
    data class Mandate(
        @SerialName("merchant_code")
        val merchantCode: String? = null,
        @SerialName("status")
        val status: String? = null,
        @SerialName("type")
        val type: String? = null
    )

    @Keep
    @Serializable
    data class Transaction(
        @SerialName("amount")
        val amount: Double? = null,
        @SerialName("auth_code")
        val authCode: String? = null,
        @SerialName("currency")
        val currency: String? = null,
        @SerialName("entry_mode")
        val entryMode: String? = null,
        @SerialName("id")
        val id: String? = null,
        @SerialName("installments_count")
        val installmentsCount: Int? = null,
        @SerialName("internal_id")
        val internalId: Int? = null,
        @SerialName("merchant_code")
        val merchantCode: String? = null,
        @SerialName("payment_type")
        val paymentType: String? = null,
        @SerialName("status")
        val status: String? = null,
        @SerialName("timestamp")
        val timestamp: String? = null,
        @SerialName("tip_amount")
        val tipAmount: Int? = null,
        @SerialName("transaction_code")
        val transactionCode: String? = null,
        @SerialName("vat_amount")
        val vatAmount: Int? = null
    )
}