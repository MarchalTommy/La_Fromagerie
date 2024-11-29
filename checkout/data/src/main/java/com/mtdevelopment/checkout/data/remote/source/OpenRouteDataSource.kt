package com.mtdevelopment.checkout.data.remote.source

import android.location.Geocoder
import android.os.Build
import com.mapbox.geojson.GeoJson
import com.mtdevelopment.checkout.data.BuildConfig
import com.mtdevelopment.checkout.data.remote.model.Constants.OPEN_ROUTE_BASE_URL_WITHOUT_HTTPS
import com.mtdevelopment.checkout.data.remote.model.request.OpenRouteRequest
import com.mtdevelopment.core.util.NetWorkResult
import com.mtdevelopment.core.util.toResultFlow
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class OpenRouteDataSource(
    private val httpClient: HttpClient,
    private val geocoder: Geocoder
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
        onSuccess: (NetWorkResult<GeoJson>) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            CoroutineScope(Dispatchers.IO).launch {
                val pairs = listOf(
                    async {
                        suspendCoroutine { coroutine ->
                            cities.forEach {
                                geocoder.getFromLocationName(it, 1) { response ->
                                    coroutine.resume(
                                        Pair(response.first().longitude, response.first().latitude)
                                    )
                                }
                            }
                        }
                    }.await()
                )
                getGeoJsonForLngLatList(pairs).collect {
                    if (it is NetWorkResult.Success) {
                        onSuccess.invoke(
                            it
                        )
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

    // TODO: FIX ISSUES CAUSED BY [0.0,0.0] and by wrong found cities (like bonnevaux).
    //  To check, use this body in the website while logged in:
    /**
     * {"coordinates": [ [0.748823, 47.812453000000005],[6.250878,46.810742],[6.289816999999999,46.826462899999996],[6.331523,46.854427],[6.303637999999999,46.81365400000001],[6.282316,46.774381],[0.0,0.0],[6.3505519999999995,46.773705],[6.316812,46.753914],[6.293718999999999,46.745706999999996],[6.192737999999999,46.710085899999996],[6.1265719999999995,46.791678999999995]]}
     */
    private fun getGeoJsonForLngLatList(lngLatList: List<Pair<Double, Double>>): Flow<NetWorkResult<GeoJson>> {
        val listOfList = lngLatList.map { pairs ->
            listOf(pairs.second, pairs.first)
        }
        return toResultFlow {
            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = OPEN_ROUTE_BASE_URL_WITHOUT_HTTPS
                    path(
                        "/v2/directions/driving-car/geojson"
                    )
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpenRouteRequest(coordinates = listOfList)
                    )
                }
            }.body<GeoJson?>()
            NetWorkResult.Success(response)
        }
    }
}