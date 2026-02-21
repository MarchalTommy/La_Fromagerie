package com.mtdevelopment.core.data

import com.google.android.gms.wallet.WalletConstants

object Constants {

    ///////////////////////////////////////////////////////////////////////////
    // GOUV ADRESSE API CONSTANTS
    ///////////////////////////////////////////////////////////////////////////
    const val ADDRESS_API_BASE_URL = "https://api-adresse.data.gouv.fr"
    const val ADDRESS_API_BASE_URL_WITHOUT_HTTPS = "api-adresse.data.gouv.fr"

    ///////////////////////////////////////////////////////////////////////////
    // GOUV AUTOCOMPLETE API CONSTANTS
    ///////////////////////////////////////////////////////////////////////////
    const val AUTOCOMPLETE_API_BASE_URL = "https://data.geopf.fr/geocodage"
    const val AUTOCOMPLETE_API_BASE_URL_WITHOUT_HTTPS = "data.geopf.fr/geocodage"

    ///////////////////////////////////////////////////////////////////////////
    // OPEN ROUTE CONSTANTS
    ///////////////////////////////////////////////////////////////////////////
    const val OPEN_ROUTE_BASE_URL = "https://api.openrouteservice.org"
    const val OPEN_ROUTE_BASE_URL_WITHOUT_HTTPS = "api.openrouteservice.org"

    ///////////////////////////////////////////////////////////////////////////
    // GOOGLE ROUTE
    ///////////////////////////////////////////////////////////////////////////
    const val GOOGLE_ROUTE_BASE_URL = "https://routes.googleapis.com"
    const val GOOGLE_ROUTE_BASE_URL_WITHOUT_HTTPS = "routes.googleapis.com"

    ///////////////////////////////////////////////////////////////////////////
    // SUMUP CONSTANTS
    ///////////////////////////////////////////////////////////////////////////
    const val SUM_UP_BASE_URL = "https://api.sumup.com"
    const val SUM_UP_BASE_URL_WITHOUT_HTTPS = "api.sumup.com"

    ///////////////////////////////////////////////////////////////////////////
    // GOOGLE PAY CONSTANTS
    ///////////////////////////////////////////////////////////////////////////

    const val PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_PRODUCTION

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