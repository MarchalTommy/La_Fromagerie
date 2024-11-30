package com.mtdevelopment.checkout.data.remote.source

import android.location.Geocoder
import android.os.Build
import com.google.gson.GsonBuilder
import com.mapbox.geojson.GeoJson
import com.mapbox.geojson.gson.GeoJsonAdapterFactory
import com.mapbox.geojson.gson.GeometryGeoJson
import com.mtdevelopment.checkout.data.BuildConfig
import com.mtdevelopment.checkout.data.remote.model.Constants.OPEN_ROUTE_BASE_URL_WITHOUT_HTTPS
import com.mtdevelopment.checkout.data.remote.model.request.OpenRouteRequest
import com.mtdevelopment.checkout.domain.model.GeoJsonFeatureCollection
import com.mtdevelopment.core.util.NetWorkResult
import com.mtdevelopment.core.util.toResultFlow
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class OpenRouteDataSource(
    private val httpClient: HttpClient,
    private val geocoder: Geocoder,
    private val json: Json
) {
    init {
        httpClient.config {
            install(DefaultRequest) {
                url {
                    protocol = URLProtocol.HTTPS
                    host = OPEN_ROUTE_BASE_URL_WITHOUT_HTTPS
                }
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        BearerTokens(BuildConfig.OPEN_ROUTE_TOKEN, null)
                    }
                }
            }
        }
    }

    suspend fun getLngLatForCities(
        cities: List<String>,
        onSuccess: (NetWorkResult<GeoJsonFeatureCollection>) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            CoroutineScope(Dispatchers.IO).launch {
                val deferredList = cities.map { city ->
                    async {
                        suspendCoroutine { continuation ->
                            geocoder.getFromLocationName(city, 1) { response ->
                                if (response.isNotEmpty()) {
                                    // TODO: Repair that : https://stackoverflow.com/questions/48227346/kotlin-coroutine-throws-java-lang-illegalstateexception-already-resumed-but-go
                                    continuation.resume(
                                        Pair(
                                            response.first().latitude,
                                            response.first().longitude
                                        )
                                    )
                                } else {
                                    // Gérer le cas où la réponse est vide
                                    continuation.resume(Pair(0.0, 0.0))
                                }
                            }
                        }
                    }
                }
                val allPairs = deferredList.awaitAll()
                getGeoJsonForLngLatList(allPairs).collect {
                    if (it is NetWorkResult.Success) {
                        onSuccess.invoke(it)
                    }
                }
            }
        } else {
            try {
                val listOfAddresses = cities.map {
                    geocoder.getFromLocationName(
                        it, 1
                    )?.firstOrNull()
                }
                val pairs = listOfAddresses.mapNotNull {
                    if (it != null) {
                        Pair(it.latitude, it.longitude)
                    } else {
                        Pair(0.0, 0.0)
                    }
                }
                getGeoJsonForLngLatList(pairs).collect {
                    if (it is NetWorkResult.Success) {
                        onSuccess.invoke(
                            it
                        )
                    }
                }
            } catch (e: Exception) {
//                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    // TODO: FIX ISSUES CAUSED BY wrong found cities (like bonnevaux).
    //  To check, use this body in the website while logged in:
    /**
     * {"coordinates": [ [0.748823, 47.812453000000005],[6.250878,46.810742],[6.289816999999999,46.826462899999996],[6.331523,46.854427],[6.303637999999999,46.81365400000001],[6.282316,46.774381],[0.0,0.0],[6.3505519999999995,46.773705],[6.316812,46.753914],[6.293718999999999,46.745706999999996],[6.192737999999999,46.710085899999996],[6.1265719999999995,46.791678999999995]]}
     */
    private fun getGeoJsonForLngLatList(lngLatList: List<Pair<Double, Double>>): Flow<NetWorkResult<GeoJsonFeatureCollection>> {
        val filteredList = lngLatList.filter { it.first != 0.0 || it.second != 0.0 }
        val listOfList = filteredList.map { pair ->
            listOf(pair.second, pair.first)
        }
        return toResultFlow {
            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = OPEN_ROUTE_BASE_URL_WITHOUT_HTTPS
                    header(
                        HttpHeaders.Accept,
                        "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8"
                    )
                    path(
                        "/v2/directions/driving-car/geojson"
                    )
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpenRouteRequest(coordinates = listOfList)
                    )
                }
            }.body<String?>()
            NetWorkResult.Success(
                json.decodeFromString<GeoJsonFeatureCollection>(response.toString())
            )
        }
    }
}