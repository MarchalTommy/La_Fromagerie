package com.mtdevelopment.checkout.data.remote.model.request

import com.mtdevelopment.checkout.domain.model.Checkout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckoutCreationBody(
    @SerialName("amount")
    val amount: Double,

    @SerialName("checkout_reference")
    val checkoutReference: String,

    @SerialName("currency")
    val currency: String,

    @SerialName("customer_id")
    val customerId: String? = null,

    @SerialName("date")
    val date: String? = null,

    @SerialName("description")
    val description: String? = null,

    @SerialName("id")
    val id: String? = null,

    @SerialName("merchant_code")
    val merchantCode: String,

    @SerialName("pay_to_email")
    val payToEmail: String? = null,

    @SerialName("payment_type")
    val paymentType: String? = null,

    @SerialName("personal_details")
    val personalDetails: PersonalDetails? = null,

    @SerialName("purpose")
    val purpose: CHECKOUT_CREATION_BODY_PURPOSE? = null,

    @SerialName("redirect_url")
    val redirectUrl: String? = null,

    @SerialName("return_url")
    val returnUrl: String? = null,

    @SerialName("status")
    val status: CHECKOUT_CREATION_BODY_STATUS? = null,

    @SerialName("transactions")
    val transactions: List<Transaction>? = null,

    @SerialName("valid_until")
    val validUntil: String? = null
)

@Serializable
data class PersonalDetails(
    @SerialName("address")
    val address: Address? = null,

    @SerialName("email")
    val email: String? = null,

    @SerialName("first_name")
    val firstName: String? = null,

    @SerialName("last_name")
    val lastName: String? = null,

    @SerialName("tax_id")
    val taxId: String? = null
)

@Serializable
data class Address(
    @SerialName("city")
    val city: String? = null,

    @SerialName("country")
    val country: String? = null,

    @SerialName("line_1")
    val firstLine: String? = null,

    @SerialName("postal_code")
    val postalCode: String? = null
)

@Serializable
data class Transaction(
    @SerialName("amount")
    val amount: Double? = null,

    @SerialName("currency")
    val currency: String? = null,

    @SerialName("id")
    val id: String? = null,

    @SerialName("installments_count")
    val installmentsCount: Int? = null,

    @SerialName("payment_type")
    val paymentType: String? = null,

    @SerialName("status")
    val status: String? = null,

    @SerialName("timestamp")
    val timestamp: String? = null,

    @SerialName("transaction_code")
    val transactionCode: String? = null,

    @SerialName("auth_code")
    val authCode: String? = null,

    @SerialName("entry_mode")
    val entryMode: String? = null,

    @SerialName("internal_id")
    val internalId: Int? = null,

    @SerialName("merchant_code")
    val merchantCode: String? = null,

    @SerialName("tip_amount")
    val tipAmount: Double? = null,

    @SerialName("vat_amount")
    val vatAmount: Double? = null
)

enum class CHECKOUT_CREATION_BODY_PURPOSE(val value: String) {
    CHECKOUT("CHECKOUT"), SETUP_RECURRING_PAYMENT("SETUP_RECURRING_PAYMENT")
}

enum class CHECKOUT_CREATION_BODY_STATUS(val value: String) {
    PENDING("PENDING"), FAILED("FAILED"), PAID("PAID")
}

fun Checkout.toCheckoutCreationBody() = CheckoutCreationBody(
    amount = amount,
    checkoutReference = checkoutReference,
    currency = currency,
    customerId = customerId,
    date = date,
    description = description,
    id = id,
    merchantCode = merchantCode,
    payToEmail = payToEmail,
    paymentType = paymentType,
    personalDetails = PersonalDetails(
        address = Address(
            city = personalDetails?.address?.city,
            country = personalDetails?.address?.country,
            firstLine = personalDetails?.address?.firstLine,
            postalCode = personalDetails?.address?.postalCode
        ),
        email = personalDetails?.email,
        firstName = personalDetails?.firstName,
        lastName = personalDetails?.lastName,
        taxId = personalDetails?.taxId,
    ),
    purpose = CHECKOUT_CREATION_BODY_PURPOSE.CHECKOUT,
    redirectUrl = redirectUrl,
    returnUrl = returnUrl,
    status = CHECKOUT_CREATION_BODY_STATUS.valueOf(status?.name ?: ""),
    transactions = transactions?.map {
        Transaction(
            amount = it.amount,
            currency = it.currency,
            id = it.id,
            installmentsCount = it.installmentsCount,
            paymentType = it.paymentType,
        )
    },
    validUntil = validUntil
)