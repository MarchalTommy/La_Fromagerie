package com.mtdevelopment.delivery.data.model

import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import com.mtdevelopment.core.data.Constants as CoreConstants

/**
 * The geocoding host lives in two `Constants` objects — one in `core/data` for autocomplete, one
 * here for address, city and street lookups — and every geocoding feature in the app breaks at once
 * if they drift apart or if a host regains a path segment.
 *
 * The old host, `api-adresse.data.gouv.fr`, was sunset on 31/01/2026 and now only proxies to the
 * Géoplateforme; when that proxy is withdrawn, anything still pointing at it goes dark. These tests
 * pin the migrated shape so the next edit cannot silently half-apply.
 */
class GeocodingHostTest {

    /**
     * Ktor's `URLBuilder.host` takes an authority, not a URL: a slash in it produces a request that
     * never reaches the API. `AUTOCOMPLETE_API_BASE_URL_WITHOUT_HTTPS` used to be
     * `"data.geopf.fr/geocodage"` for exactly this reason, which is why the path prefix is a
     * separate constant.
     */
    @Test
    fun `host constants carry no scheme and no path`() {
        listOf(
            Constants.ADDRESS_API_BASE_URL_WITHOUT_HTTPS,
            CoreConstants.ADDRESS_API_BASE_URL_WITHOUT_HTTPS
        ).forEach { host ->
            assertFalse("Host must not contain a path separator: $host", host.contains("/"))
            assertFalse("Host must not contain a scheme: $host", host.contains(":"))
        }
    }

    @Test
    fun `both modules point at the same geocoding host and prefix`() {
        assertEquals(
            CoreConstants.ADDRESS_API_BASE_URL_WITHOUT_HTTPS,
            Constants.ADDRESS_API_BASE_URL_WITHOUT_HTTPS
        )
        assertEquals(CoreConstants.GEOCODAGE_PATH_PREFIX, Constants.GEOCODAGE_PATH_PREFIX)
    }

    /**
     * Composes the URL the same way every call site does, so a host or prefix change has to produce
     * the endpoint shape that was verified against the live API.
     */
    @Test
    fun `search endpoint resolves to the geoplateforme geocoding path`() {
        val url = URLBuilder().apply {
            protocol = URLProtocol.HTTPS
            host = Constants.ADDRESS_API_BASE_URL_WITHOUT_HTTPS
            encodedPath = "${Constants.GEOCODAGE_PATH_PREFIX}/search/?q=Frasne-25560&type=municipality"
        }.build().toString()

        assertEquals(
            "https://data.geopf.fr/geocodage/search/?q=Frasne-25560&type=municipality",
            url
        )
    }

    @Test
    fun `completion endpoint resolves to the geoplateforme geocoding path`() {
        val url = URLBuilder().apply {
            protocol = URLProtocol.HTTPS
            host = CoreConstants.ADDRESS_API_BASE_URL_WITHOUT_HTTPS
            encodedPath = "${CoreConstants.GEOCODAGE_PATH_PREFIX}/completion/?text=Frasn&terr=25%2C39"
        }.build().toString()

        assertEquals("https://data.geopf.fr/geocodage/completion/?text=Frasn&terr=25%2C39", url)
    }
}
