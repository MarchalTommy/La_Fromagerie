package com.mtdevelopment.checkout.domain.model

data class GooglePayData(
    val apiVersion: Int?,
    val apiVersionMinor: Int?,
    val paymentMethodData: PaymentMethodData?
) {
    data class PaymentMethodData(
        val description: String?,
        val info: Info?,
        val tokenizationData: TokenizationData?,
        val type: String?
    ) {
        data class Info(
            val billingAddress: BillingAddress?,
            val cardDetails: String?,
            val cardNetwork: String?
        ) {
            data class BillingAddress(
                val phoneNumber: String?
            )
        }

        data class TokenizationData(
            val token: String?,
            val type: String?
        )
    }
}