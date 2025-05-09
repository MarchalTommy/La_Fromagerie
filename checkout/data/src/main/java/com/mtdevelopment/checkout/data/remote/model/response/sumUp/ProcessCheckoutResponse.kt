package com.mtdevelopment.checkout.data.remote.model.response.sumUp


import com.mtdevelopment.checkout.domain.model.ProcessCheckoutResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProcessCheckoutResponse(
    @SerialName("next_step")
    val nextStep: NextStep? = null
) {
    @Serializable
    data class NextStep(
        @SerialName("full")
        val full: String? = null,
        @SerialName("mechanism")
        val mechanism: List<String?>? = null,
        @SerialName("method")
        val method: String? = null,
        @SerialName("payload")
        val payload: Payload? = null,
        @SerialName("url")
        val url: String? = null
    ) {
        @Serializable
        data class Payload(
            @SerialName("cs")
            val cs: String? = null,
            @SerialName("rs")
            val rs: String? = null,
            @SerialName("tx")
            val tx: String? = null
        )
    }
}

fun ProcessCheckoutResponse.toProcessCheckoutResult() = ProcessCheckoutResult(
    nextStep = this.nextStep?.toNextStep()
)

fun ProcessCheckoutResponse.NextStep.toNextStep() = ProcessCheckoutResult.NextStep(
    full = this.full,
    mechanism = this.mechanism,
    method = this.method,
    payload = this.payload?.toPayload(),
    url = this.url
)

fun ProcessCheckoutResponse.NextStep.Payload.toPayload() = ProcessCheckoutResult.NextStep.Payload(
    cs = this.cs,
    rs = this.rs,
    tx = this.tx
)