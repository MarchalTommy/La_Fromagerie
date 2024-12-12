package com.mtdevelopment.checkout.data.remote.source

import com.mtdevelopment.checkout.data.remote.model.Constants.ADDRESS_API_BASE_URL_WITHOUT_HTTPS
import com.mtdevelopment.checkout.data.remote.model.response.addressData.AddressData
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

class AddressApiDataSource(
    private val httpClient: HttpClient,
    private val json: Json
) {

    suspend fun getLngLatFromCity(cityName: String, zip: Int): NetWorkResult<AddressData> {
        val response = httpClient.get {
            url {
                protocol = URLProtocol.HTTPS
                host = ADDRESS_API_BASE_URL_WITHOUT_HTTPS
                header(
                    HttpHeaders.Accept,
                    "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8"
                )
                encodedPath = "/search/?q=${cityName.encodeURLPathPart()}-${zip}&type=municipality"
            }
        }.body<String?>()
        return NetWorkResult.Success(
            json.decodeFromString<AddressData>(response.toString())
        )
    }
}