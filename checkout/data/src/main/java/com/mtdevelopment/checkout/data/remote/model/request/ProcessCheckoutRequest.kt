package com.mtdevelopment.checkout.data.remote.model.request


import com.mtdevelopment.checkout.domain.model.GooglePayData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProcessCheckoutRequest(
    @SerialName("currency")
    val currency: String?,
    @SerialName("google_pay")
    val googlePay: GooglePay?,
    @SerialName("id")
    val id: String?,
    @SerialName("payment_type")
    val paymentType: String? = "google_pay"
) {
    @Serializable
    data class GooglePay(
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
                @SerialName("cardDetails")
                val cardDetails: String?,
                @SerialName("cardNetwork")
                val cardNetwork: String?
            )

            @Serializable
            data class TokenizationData(
                @SerialName("token")
                val token: String?,
                @SerialName("type")
                val type: String?
            )
        }
    }
}

fun GooglePayData.PaymentMethodData.toPaymentData() =
    ProcessCheckoutRequest.GooglePay.PaymentMethodData(
        description = description,
        info = ProcessCheckoutRequest.GooglePay.PaymentMethodData.Info(
            cardDetails = info?.cardDetails,
            cardNetwork = info?.cardNetwork
        ),
        tokenizationData = ProcessCheckoutRequest.GooglePay.PaymentMethodData.TokenizationData(
            token = tokenizationData?.token,
            type = tokenizationData?.type
        ),
        type = type
    )