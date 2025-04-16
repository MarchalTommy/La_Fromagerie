package com.mtdevelopment.checkout.data.remote.model.response.sumUp


import androidx.annotation.Keep
import com.mtdevelopment.checkout.domain.model.NewCheckoutResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Keep
data class NewCheckoutResponse(
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
    @SerialName("merchant_country")
    val merchantCountry: String? = null,
    @SerialName("pay_to_email")
    val payToEmail: String? = null,
    @SerialName("return_url")
    val returnUrl: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("transactions")
    val transactions: List<Transaction?>? = null,
    @SerialName("valid_until")
    val validUntil: String? = null
) {
    @Serializable
    @Keep
    data class Mandate(
        @SerialName("merchant_code")
        val merchantCode: String? = null,
        @SerialName("status")
        val status: String? = null,
        @SerialName("type")
        val type: String? = null
    )

    @Serializable
    @Keep
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