package com.mtdevelopment.checkout.data.remote.model.response.sumUp


import androidx.annotation.Keep
import com.mtdevelopment.checkout.domain.model.NewCheckoutResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Keep
data class NewCheckoutResponse(
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
    @SerialName("merchant_country")
    val merchantCountry: String?,
    @SerialName("pay_to_email")
    val payToEmail: String?,
    @SerialName("return_url")
    val returnUrl: String?,
    @SerialName("status")
    val status: String?,
    @SerialName("transactions")
    val transactions: List<Transaction?>?,
    @SerialName("valid_until")
    val validUntil: String?
) {
    @Serializable
    @Keep
    data class Mandate(
        @SerialName("merchant_code")
        val merchantCode: String?,
        @SerialName("status")
        val status: String?,
        @SerialName("type")
        val type: String?
    )

    @Serializable
    @Keep
    data class Transaction(
        @SerialName("amount")
        val amount: Double?,
        @SerialName("auth_code")
        val authCode: String?,
        @SerialName("currency")
        val currency: String?,
        @SerialName("entry_mode")
        val entryMode: String?,
        @SerialName("id")
        val id: String?,
        @SerialName("installments_count")
        val installmentsCount: Int?,
        @SerialName("internal_id")
        val internalId: Int?,
        @SerialName("merchant_code")
        val merchantCode: String?,
        @SerialName("payment_type")
        val paymentType: String?,
        @SerialName("status")
        val status: String?,
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

fun NewCheckoutResponse.toNewCheckoutResult() = NewCheckoutResult(
    amount = this.amount,
    checkoutReference = this.checkoutReference,
    currency = this.currency,
    customerId = this.customerId,
    date = this.date,
    description = this.description,
    id = this.id,
    mandate = this.mandate?.toMandateResult(),
    merchantCode = this.merchantCode,
    merchantCountry = this.merchantCountry,
    payToEmail = this.payToEmail,
    returnUrl = this.returnUrl,
    status = this.status,
    transactions = this.transactions?.map { it?.toTransactionResult() },
    validUntil = this.validUntil
)

fun NewCheckoutResponse.Mandate.toMandateResult() = NewCheckoutResult.Mandate(
    merchantCode = this.merchantCode,
    status = this.status,
    type = this.type
)

fun NewCheckoutResponse.Transaction.toTransactionResult() = NewCheckoutResult.Transaction(
    amount = this.amount,
    authCode = this.authCode,
    currency = this.currency,
    entryMode = this.entryMode,
    id = this.id,
    installmentsCount = this.installmentsCount,
    internalId = this.internalId,
    merchantCode = this.merchantCode,
    paymentType = this.paymentType,
    status = this.status,
    timestamp = this.timestamp,
    tipAmount = this.tipAmount,
    transactionCode = this.transactionCode,
    vatAmount = this.vatAmount
)