package com.mtdevelopment.checkout.data.remote.model.response.sumUp


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProcessCheckoutResponse(
    @SerialName("next_step")
    val nextStep: NextStep?
) {
    @Serializable
    data class NextStep(
        @SerialName("full")
        val full: String?,
        @SerialName("mechanism")
        val mechanism: List<String?>?,
        @SerialName("method")
        val method: String?,
        @SerialName("payload")
        val payload: Payload?,
        @SerialName("url")
        val url: String?
    ) {
        @Serializable
        data class Payload(
            @SerialName("cs")
            val cs: String?,
            @SerialName("rs")
            val rs: String?,
            @SerialName("tx")
            val tx: String?
        )
    }
}