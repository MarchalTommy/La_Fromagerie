package com.mtdevelopment.delivery.data.source.remote

import com.mtdevelopment.core.util.NetWorkResult
import com.mtdevelopment.delivery.data.model.request.OpenRouteRequest
import com.mtdevelopment.delivery.domain.model.GeoJsonFeatureCollection
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.serialization.json.Json

class OpenRouteDataSource(
    private val httpClient: HttpClient,
    private val json: Json
) {

    suspend fun getGeoJsonForLngLatList(lngLatList: List<Pair<Double, Double>>): NetWorkResult<GeoJsonFeatureCollection> {
        val filteredList = lngLatList.filter { it.first != 0.0 || it.second != 0.0 }
        val listOfList = filteredList.map { pair ->
            listOf(pair.second, pair.first)
        }

        val response = httpClient.post {
            url {
                header(
                    HttpHeaders.Accept,
                    "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8"
                )
                path(
                    "/v2/directions/driving-car/geojson"
                )
                contentType(ContentType.Application.Json)
                setBody(
                    OpenRouteRequest(
                        coordinates = listOfList
                    )
                )
            }
        }
        return try {
            val result = json.decodeFromString<GeoJsonFeatureCollection>(response.body())
            NetWorkResult.Success(
                result
            )
        } catch (e: Exception) {
            NetWorkResult.Error(response.status.toString(), e.message ?: "")
        }
    }
}