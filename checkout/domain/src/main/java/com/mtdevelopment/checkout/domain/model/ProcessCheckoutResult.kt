package com.mtdevelopment.checkout.domain.model

data class ProcessCheckoutResult(
    val nextStep: NextStep?
) {
    data class NextStep(
//        val full: String?,
        val mechanism: List<String?>?,
        val method: String?,
        val payload: Payload?,
        val url: String?,
        val redirectUrl: String? = null
    ) {
        data class Payload(
            val paReq: String? = null,
            val md: String? = null,
            val termUrl: String? = null,
//            val cs: String?,
//            val rs: String?,
//            val tx: String?
        )
    }
}
