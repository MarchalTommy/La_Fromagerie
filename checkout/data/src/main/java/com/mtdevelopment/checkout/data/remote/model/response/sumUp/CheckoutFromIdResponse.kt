package com.mtdevelopment.checkout.data.remote.model.response.sumUp


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckoutFromIdResponse(
    @SerialName("amount")
    val amount: Double?,
    @SerialName("checkout_reference")
    val checkoutReference: String?,
    @SerialName("currency")
    val currency: String?,
    @SerialName("customer_id")
    val customerId: String?,
    @SerialName("date")
    val date: String?,
    @SerialName("description")
    val description: String?,
    @SerialName("id")
    val id: String?,
    @SerialName("mandate")
    val mandate: Mandate?,
    @SerialName("merchant_code")
    val merchantCode: String?,
    @SerialName("merchant_name")
    val merchantName: String?,
    @SerialName("pay_to_email")
    val payToEmail: String?,
    @SerialName("payment_instrument")
    val paymentInstrument: PaymentInstrument?,
    @SerialName("redirect_url")
    val redirectUrl: String?,
    @SerialName("return_url")
    val returnUrl: String?,
    @SerialName("status")
    val status: CHECKOUT_STATUS?,
    @SerialName("transaction_code")
    val transactionCode: String?,
    @SerialName("transaction_id")
    val transactionId: String?,
    @SerialName("transactions")
    val transactions: List<Transaction?>?,
    @SerialName("valid_until")
    val validUntil: String?
) {
    @Serializable
    data class Mandate(
        @SerialName("merchant_code")
        val merchantCode: String?,
        @SerialName("status")
        val status: String?,
        @SerialName("type")
        val type: String?
    )

    @Serializable
    data class PaymentInstrument(
        @SerialName("token")
        val token: String?
    )

    @Serializable
    data class Transaction(
        @SerialName("amount")
        val amount: Double?,
        @SerialName("auth_code")
        val authCode: String?,
        @SerialName("currency")
        val currency: String?,
        @SerialName("entry_mode")
        val entryMode: CHECKOUT_TRANSACTION_ENTRY_MODE?,
        @SerialName("id")
        val id: String?,
        @SerialName("installments_count")
        val installmentsCount: Int?,
        @SerialName("internal_id")
        val internalId: Int?,
        @SerialName("merchant_code")
        val merchantCode: String?,
        @SerialName("payment_type")
        val paymentType: CHECKOUT_TRANSACTION_PAYMENT_TYPE?,
        @SerialName("status")
        val status: CHECKOUT_TRANSACTION_STATUS?,
        @SerialName("timestamp")
        val timestamp: String?,
        @SerialName("tip_amount")
        val tipAmount: Int?,
        @SerialName("transaction_code")
        val transactionCode: String?,
        @SerialName("vat_amount")
        val vatAmount: Int?
    )
}