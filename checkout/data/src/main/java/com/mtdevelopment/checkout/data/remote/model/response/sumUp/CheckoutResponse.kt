package com.mtdevelopment.checkout.data.remote.model.response.sumUp


import com.mtdevelopment.checkout.domain.model.Checkout
import com.mtdevelopment.checkout.domain.model.Transaction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckoutResponse(
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
    @SerialName("pay_to_email")
    val payToEmail: String?,
    @SerialName("return_url")
    val returnUrl: String?,
    @SerialName("status")
    val status: CHECKOUT_STATUS?,
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

@Serializable
enum class CHECKOUT_STATUS(val value: String) {
    PENDING("PENDING"), FAILED("FAILED"), PAID("PAID")
}

@Serializable
enum class CHECKOUT_TRANSACTION_STATUS(val value: String) {
    PENDING("PENDING"), FAILED("FAILED"), SUCCESSFUL("SUCCESSFUL"), CANCELLED("CANCELLED")
}

@Serializable
enum class CHECKOUT_TRANSACTION_ENTRY_MODE(val value: String) {
    CUSTOMER_ENTRY("CUSTOMER_ENTRY"), BOLETO("BOLETO")
}

@Serializable
enum class CHECKOUT_TRANSACTION_PAYMENT_TYPE(val value: String) {
    ECOM("ECOM"), RECURRING("RECURRING"), BOLETO("BOLETO")
}

/**
 * Mappe un objet CheckoutResponse.Transaction (source) vers un objet Transaction (cible).
 */
fun CheckoutResponse.Transaction.toDomainTransaction(): Transaction {
    return Transaction(
        amount = this.amount,
        currency = this.currency,
        id = this.id,
        installmentsCount = this.installmentsCount,
        paymentType = this.paymentType?.value, // Mappe l'enum source vers sa valeur String
        status = this.status?.value,           // Mappe l'enum source vers sa valeur String
        timestamp = this.timestamp,
        transactionCode = this.transactionCode,
        authCode = this.authCode,
        entryMode = this.entryMode?.value,     // Mappe l'enum source vers sa valeur String
        internalId = this.internalId,
        merchantCode = this.merchantCode,
        tipAmount = this.tipAmount?.toDouble(), // Convertit Int? en Double?
        vatAmount = this.vatAmount?.toDouble()  // Convertit Int? en Double?
    )
}

fun CheckoutResponse.toDomainCheckout(): Checkout {
    val domainAmount = this.amount
        ?: throw IllegalArgumentException("CheckoutResponse.amount ne peut pas être nul lors du mappage vers Checkout.amount")
    val domainCheckoutReference = this.checkoutReference
        ?: throw IllegalArgumentException("CheckoutResponse.checkoutReference ne peut pas être nul lors du mappage vers Checkout.checkoutReference")
    val domainCurrency = this.currency
        ?: throw IllegalArgumentException("CheckoutResponse.currency ne peut pas être nul lors du mappage vers Checkout.currency")
    val domainMerchantCode = this.merchantCode
        ?: throw IllegalArgumentException("CheckoutResponse.merchantCode ne peut pas être nul lors du mappage vers Checkout.merchantCode")

    val domainTransactions: List<Transaction>? = this.transactions
        ?.mapNotNull { it?.toDomainTransaction() }

    // Dérive paymentType de la première transaction, si disponible
    val paymentTypeFromTransaction = domainTransactions?.firstOrNull()?.paymentType

    val domainStatus: com.mtdevelopment.checkout.domain.model.CHECKOUT_STATUS? =
        this.status?.let { sourceStatus ->
            enumValues<com.mtdevelopment.checkout.domain.model.CHECKOUT_STATUS>().find { it.value == sourceStatus.value }
        }

    return Checkout(
        amount = domainAmount,
        checkoutReference = domainCheckoutReference,
        currency = domainCurrency,
        customerId = this.customerId,
        date = this.date,
        description = this.description,
        id = this.id,
        merchantCode = domainMerchantCode,
        payToEmail = this.payToEmail,
        paymentType = paymentTypeFromTransaction,
        personalDetails = null, // Pas de mappage direct depuis CheckoutResponse pour personalDetails
        purpose = null,         // Pas de mappage direct pour purpose; CHECKOUT_PURPOSE est un nouvel enum
        redirectUrl = null,     // Pas de mappage direct depuis CheckoutResponse pour redirectUrl
        returnUrl = this.returnUrl,
        status = domainStatus,
        transactions = domainTransactions,
        validUntil = this.validUntil
    )
}