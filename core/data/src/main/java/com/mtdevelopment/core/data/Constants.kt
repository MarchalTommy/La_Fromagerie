package com.mtdevelopment.core.data

import com.google.android.gms.wallet.WalletConstants

object Constants {

    ///////////////////////////////////////////////////////////////////////////
    // GOUV ADRESSE API CONSTANTS
    ///////////////////////////////////////////////////////////////////////////
    // The old host, api-adresse.data.gouv.fr, was sunset on 31/01/2026 and now only
    // proxies to the Géoplateforme. Everything geocoding — address matching, city
    // lookup, street suggestions, autocomplete — goes through data.geopf.fr, where the
    // same endpoints live one level down, under /geocodage.
    // The host must stay path-free: Ktor's URLBuilder.host rejects a slash, so the
    // prefix belongs in encodedPath (see GEOCODAGE_PATH_PREFIX).
    const val ADDRESS_API_BASE_URL = "https://data.geopf.fr/geocodage"
    const val ADDRESS_API_BASE_URL_WITHOUT_HTTPS = "data.geopf.fr"
    const val GEOCODAGE_PATH_PREFIX = "/geocodage"

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