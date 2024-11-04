package com.mtdevelopment.checkout.domain.model

data class NewCheckoutResult(
    val amount: Double?,
    val checkoutReference: String?,
    val currency: String?,
    val customerId: String?,
    val date: String?,
    val description: String?,
    val id: String?,
    val mandate: Mandate?,
    val merchantCode: String?,
    val merchantCountry: String?,
    val payToEmail: String?,
    val returnUrl: String?,
    val status: String?,
    val transactions: List<Transaction?>?,
    val validUntil: String?
) {
    data class Mandate(
        val merchantCode: String?,
        val status: String?,
        val type: String?
    )

    data class Transaction(
        val amount: Double?,
        val authCode: String?,
        val currency: String?,
        val entryMode: String?,
        val id: String?,
        val installmentsCount: Int?,
        val internalId: Int?,
        val merchantCode: String?,
        val paymentType: String?,
        val status: String?,
        val timestamp: String?,
        val tipAmount: Int?,
        val transactionCode: String?,
        val vatAmount: Int?
    )
}
