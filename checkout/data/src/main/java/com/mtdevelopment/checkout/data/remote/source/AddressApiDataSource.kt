package com.mtdevelopment.checkout.data.remote.source

import com.mtdevelopment.checkout.data.remote.model.Constants.ADDRESS_API_BASE_URL_WITHOUT_HTTPS
import com.mtdevelopment.checkout.data.remote.model.response.addressData.AddressData
import com.mtdevelopment.core.util.NetWorkResult
import com.mtdevelopment.core.util.toResultFlow
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.path
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

class AddressApiDataSource(
    private val httpClient: HttpClient,
    private val json: Json
) {
    init {
        httpClient.config {
            install(DefaultRequest) {
                url {
                    protocol = URLProtocol.HTTPS
                    host = ADDRESS_API_BASE_URL_WITHOUT_HTTPS
                }
            }
        }
    }

    suspend fun getLngLatFromCity(cityName: String, zip: Int): Flow<NetWorkResult<AddressData>> {
        return toResultFlow {
            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = ADDRESS_API_BASE_URL_WITHOUT_HTTPS
                    header(
                        HttpHeaders.Accept,
                        "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8"
                    )
                    path(
                        "/search/?q=${cityName}-${zip}&type=municipality"
                    )
                }
            }.body<String?>()
            NetWorkResult.Success(
                json.decodeFromString<AddressData>(response.toString())
            )
        }
    }
}