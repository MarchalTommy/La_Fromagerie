package com.mtdevelopment.checkout.domain.model

data class Checkout(
    val amount: Double,
    val checkoutReference: String,
    val currency: String,
    val customerId: String? = null,
    val date: String? = null,
    val description: String? = null,
    val id: String? = null,
    val merchantCode: String,
    val payToEmail: String? = null,
    val paymentType: String? = null,
    val personalDetails: PersonalDetails? = null,
    val purpose: CHECKOUT_PURPOSE? = null,
    val redirectUrl: String? = null,
    val returnUrl: String? = null,
    val status: CHECKOUT_STATUS? = null,
    val transactions: List<Transaction>? = null,
    val validUntil: String? = null
)

data class PersonalDetails(
    val address: Address? = null,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val taxId: String? = null
)

data class Address(
    val city: String? = null,
    val country: String? = null,
    val firstLine: String? = null,
    val postalCode: String? = null
)

data class Transaction(
    val amount: Double? = null,
    val currency: String? = null,
    val id: String? = null,
    val installmentsCount: Int? = null,
    val paymentType: String? = null,
    val status: String? = null,
    val timestamp: String? = null,
    val transactionCode: String? = null,
    val authCode: String? = null,
    val entryMode: String? = null,
    val internalId: Int? = null,
    val merchantCode: String? = null,
    val tipAmount: Double? = null,
    val vatAmount: Double? = null
)

enum class CHECKOUT_PURPOSE(val value: String) {
    CHECKOUT("CHECKOUT"), SETUP_RECURRING_PAYMENT("SETUP_RECURRING_PAYMENT")
}

enum class CHECKOUT_STATUS(val value: String) {
    PENDING("PENDING"), FAILED("FAILED"), PAID("PAID")
}