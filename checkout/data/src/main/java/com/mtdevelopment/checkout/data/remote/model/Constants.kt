package com.mtdevelopment.checkout.data.remote.model

import com.google.android.gms.wallet.WalletConstants

object Constants {

    ///////////////////////////////////////////////////////////////////////////
    // OPEN ROUTE CONSTANTS
    ///////////////////////////////////////////////////////////////////////////
    const val OPEN_ROUTE_BASE_URL = "https://api.openrouteservice.org"
    const val OPEN_ROUTE_BASE_URL_WITHOUT_HTTPS = "api.openrouteservice.org"

    ///////////////////////////////////////////////////////////////////////////
    // SUMUP CONSTANTS
    ///////////////////////////////////////////////////////////////////////////
    const val BASE_URL = "https://api.sumup.com"
    const val BASE_URL_WITHOUT_HTTPS = "api.sumup.com"

    ///////////////////////////////////////////////////////////////////////////
    // GOOGLE PAY CONSTANTS
    ///////////////////////////////////////////////////////////////////////////

    const val PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_TEST

    val SUPPORTED_NETWORKS = listOf(
        "MASTERCARD",
        "VISA"
    )

    val SUPPORTED_METHODS = listOf(
        "PAN_ONLY",
        "CRYPTOGRAM_3DS"
    )

    const val COUNTRY_CODE = "FR"

    const val CURRENCY_CODE = "EUR"

    val SHIPPING_SUPPORTED_COUNTRIES = listOf("FR")

    private const val PAYMENT_GATEWAY_TOKENIZATION_NAME = "sumup"

    val PAYMENT_GATEWAY_TOKENIZATION_PARAMETERS = mapOf(
        "gateway" to PAYMENT_GATEWAY_TOKENIZATION_NAME,
        "gatewayMerchantId" to "MFHN73AC"
    )
}