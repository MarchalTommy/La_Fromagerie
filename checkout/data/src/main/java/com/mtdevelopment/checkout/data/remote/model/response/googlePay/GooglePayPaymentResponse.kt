package com.mtdevelopment.checkout.data.remote.model.response.googlePay


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GooglePayPaymentResponse(
    @SerialName("apiVersion")
    val apiVersion: Int?,
    @SerialName("apiVersionMinor")
    val apiVersionMinor: Int?,
    @SerialName("paymentMethodData")
    val paymentMethodData: PaymentMethodData?
) {
    @Serializable
    data class PaymentMethodData(
        @SerialName("description")
        val description: String?,
        @SerialName("info")
        val info: Info?,
        @SerialName("tokenizationData")
        val tokenizationData: TokenizationData?,
        @SerialName("type")
        val type: String?
    ) {
        @Serializable
        data class Info(
            @SerialName("billingAddress")
            val billingAddress: BillingAddress?,
            @SerialName("cardDetails")
            val cardDetails: String?,
            @SerialName("cardNetwork")
            val cardNetwork: String?
        ) {
            @Serializable
            data class BillingAddress(
                @SerialName("phoneNumber")
                val phoneNumber: String?
            )
        }

        @Serializable
        data class TokenizationData(
            @SerialName("token")
            val token: String?,
            @SerialName("type")
            val type: String?
        )
    }
}