package com.mtdevelopment.delivery.data.source.remote

import com.mtdevelopment.core.util.NetWorkResult
import com.mtdevelopment.delivery.data.model.Constants.ADDRESS_API_BASE_URL_WITHOUT_HTTPS
import com.mtdevelopment.delivery.data.model.response.autocomplete.AutoCompleteSuggestions
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.encodeURLPathPart
import io.ktor.http.encodedPath
import kotlinx.serialization.json.Json

class AutoCompleteApiDataSource(
    private val httpClient: HttpClient,
    private val json: Json
) {

    suspend fun getAutoCompleteSuggestions(query: String): NetWorkResult<AutoCompleteSuggestions> {
        val response = httpClient.get {
            url {
                protocol = URLProtocol.HTTPS
                host =
                    ADDRESS_API_BASE_URL_WITHOUT_HTTPS
                header(
                    HttpHeaders.Accept,
                    "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8"
                )
                encodedPath =
                    "/completion/?text=${query.encodeURLPathPart()}&terr=25%2C39&poiType=${"zone d'habitation".encodeURLPathPart()}&type=StreetAddress&maximumResponses=3"
            }
        }

        return try {
            NetWorkResult.Success(
                response.body<AutoCompleteSuggestions>()
            )
        } catch (e: Exception) {
            NetWorkResult.Error(response.status.toString(), e.message ?: "")
        }
    }
}