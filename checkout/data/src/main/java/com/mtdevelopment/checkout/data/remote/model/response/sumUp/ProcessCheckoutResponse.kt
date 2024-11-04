package com.mtdevelopment.checkout.data.remote.model.response.sumUp


import com.mtdevelopment.checkout.domain.model.ProcessCheckoutResult
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