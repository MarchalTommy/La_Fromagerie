package com.mtdevelopment.checkout.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GooglePayData(
    val apiVersion: Int? = null,
    val apiVersionMinor: Int? = null,
    val paymentMethodData: PaymentMethodData? = null
) {
    @Serializable
    data class PaymentMethodData(
        val description: String? = null,
        val info: Info? = null,
        val tokenizationData: TokenizationData? = null,
        val type: String? = null
    ) {
        @Serializable
        data class Info(
            val billingAddress: BillingAddress? = null,
            val cardDetails: String? = null,
            val cardNetwork: String? = null
        ) {
            @Serializable
            data class BillingAddress(
                val phoneNumber: String? = null
            )
        }

        @Serializable
        data class TokenizationData(
            val token: String? = null,
            val type: String? = null
        )
    }
}