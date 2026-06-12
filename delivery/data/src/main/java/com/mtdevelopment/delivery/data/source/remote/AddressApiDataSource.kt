package com.mtdevelopment.delivery.data.source.remote

import com.mtdevelopment.core.util.NetWorkResult
import com.mtdevelopment.delivery.data.model.Constants.ADDRESS_API_BASE_URL_WITHOUT_HTTPS
import com.mtdevelopment.delivery.data.model.response.addressData.AddressData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.encodeURLPathPart
import io.ktor.http.encodedPath
import kotlinx.serialization.json.Json

/**
 * Data source for geocoding services, utilizing the French Government's Address API (adresse.data.gouv.fr).
 * It translates physical addresses or city names into geographic coordinates (Latitude, Longitude).
 */
class AddressApiDataSource(
    private val httpClient: HttpClient,
    private val json: Json
) {

    /**
     * Retrieves coordinates for a specific city and zip code.
     * Uses the "municipality" type to get the center of the town.
     */
    suspend fun getLngLatFromCity(cityName: String, zip: Int): NetWorkResult<AddressData> {
        // The request itself is inside the try so network failures surface as
        // NetWorkResult.Error instead of crashing the caller.
        return try {
            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host =
                        ADDRESS_API_BASE_URL_WITHOUT_HTTPS
                    header(
                        HttpHeaders.Accept,
                        "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8"
                    )
                    // Encoding search query for safer URL handling
                    encodedPath = "/search/?q=${cityName.encodeURLPathPart()}-${zip}&type=municipality"
                }
            }
            NetWorkResult.Success(
                response.body<AddressData>()
            )
        } catch (e: Exception) {
            NetWorkResult.Error(e.message ?: "Unknown address API error", e::class.simpleName)
        }
    }

    /**
     * Retrieves coordinates for a full street address.
     */
    suspend fun getLngLatFromAddress(address: String): NetWorkResult<AddressData> {
        return try {
            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host =
                        ADDRESS_API_BASE_URL_WITHOUT_HTTPS
                    header(
                        HttpHeaders.Accept,
                        "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8"
                    )
                    encodedPath = "/search/?q=${address.encodeURLPathPart()}&type=municipality"
                }
            }
            NetWorkResult.Success(
                response.body<AddressData>()
            )
        } catch (e: Exception) {
            NetWorkResult.Error(e.message ?: "Unknown address API error", e::class.simpleName)
        }
    }
}