package com.mtdevelopment.delivery.data.model

object Constants {

    ///////////////////////////////////////////////////////////////////////////
    // GOUV ADRESSE API CONSTANTS
    ///////////////////////////////////////////////////////////////////////////
    // Kept in sync with core/data Constants — the old api-adresse.data.gouv.fr host was
    // sunset on 31/01/2026 and only proxies to the Géoplateforme now. Host stays
    // path-free (Ktor's URLBuilder.host rejects a slash); the /geocodage prefix goes on
    // encodedPath at each call site.
    const val ADDRESS_API_BASE_URL = "https://data.geopf.fr/geocodage"
    const val ADDRESS_API_BASE_URL_WITHOUT_HTTPS = "data.geopf.fr"
    const val GEOCODAGE_PATH_PREFIX = "/geocodage"

    ///////////////////////////////////////////////////////////////////////////
    // OPEN ROUTE CONSTANTS
    ///////////////////////////////////////////////////////////////////////////
    const val OPEN_ROUTE_BASE_URL = "https://api.openrouteservice.org"
    const val OPEN_ROUTE_BASE_URL_WITHOUT_HTTPS = "api.openrouteservice.org"
}