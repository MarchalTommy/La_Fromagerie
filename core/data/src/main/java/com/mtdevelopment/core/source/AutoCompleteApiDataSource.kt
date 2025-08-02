package com.mtdevelopment.core.source

import com.mtdevelopment.core.data.Constants.ADDRESS_API_BASE_URL_WITHOUT_HTTPS
import com.mtdevelopment.core.model.autocomplete.AutoCompleteSuggestions
import com.mtdevelopment.core.util.NetWorkResult
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

    suspend fun getAutoCompleteSuggestions(query: String): NetWorkResult<Any> {
        val response = httpClient.get {
            url {
                protocol = URLProtocol.Companion.HTTPS
                host = ADDRESS_API_BASE_URL_WITHOUT_HTTPS
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
            NetWorkResult.Error(response.status, e.message ?: "")
        }
    }
}