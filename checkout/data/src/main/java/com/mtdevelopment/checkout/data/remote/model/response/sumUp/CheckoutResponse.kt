package com.mtdevelopment.checkout.data.remote.model.response.sumUp


import com.mtdevelopment.checkout.domain.model.Checkout
import com.mtdevelopment.checkout.domain.model.Transaction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckoutResponse(
    @SerialName("amount")
    val amount: Double? = null,
    @SerialName("checkout_reference")
    val checkoutReference: String? = null,
    @SerialName("currency")
    val currency: String? = null,
    @SerialName("customer_id")
    val customerId: String? = null, // Ajout de = null
    @SerialName("date")
    val date: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("id")
    val id: String? = null,
    @SerialName("mandate")
    val mandate: Mandate? = null, // Ajout de = null
    @SerialName("merchant_code")
    val merchantCode: String? = null,
    @SerialName("pay_to_email")
    val payToEmail: String? = null,
    @SerialName("return_url")
    val returnUrl: String? = null, // Ajout de = null
    @SerialName("status")
    val status: CHECKOUT_STATUS? = null,
    @SerialName("transactions")
    val transactions: List<Transaction?>? = null,
    @SerialName("valid_until")
    val validUntil: String? = null // Ajout de = null
) {
    @Serializable
    data class Mandate(
        @SerialName("merchant_code")
        val merchantCode: String? = null,
        @SerialName("status")
        val status: String? = null,
        @SerialName("type")
        val type: String? = null
    )

    @Serializable
    data class Transaction(
        @SerialName("amount")
        val amount: Double? = null,
        @SerialName("auth_code")
        val authCode: String? = null,
        @SerialName("currency")
        val currency: String? = null,
        @SerialName("entry_mode")
        val entryMode: CHECKOUT_TRANSACTION_ENTRY_MODE? = null,
        @SerialName("id")
        val id: String? = null,
        @SerialName("installments_count")
        val installmentsCount: Int? = null,
        @SerialName("internal_id")
        val internalId: Long? = null,
        @SerialName("merchant_code")
        val merchantCode: String? = null,
        @SerialName("payment_type")
        val paymentType: CHECKOUT_TRANSACTION_PAYMENT_TYPE? = null,
        @SerialName("status")
        val status: CHECKOUT_TRANSACTION_STATUS? = null,
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
    CUSTOMER_ENTRY("CUSTOMER_ENTRY"), BOLETO("BOLETO"), GOOGLE_PAY("GOOGLE_PAY")
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