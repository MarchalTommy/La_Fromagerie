package com.mtdevelopment.admin.data.source

import com.mtdevelopment.admin.data.BuildConfig.GOOGLE_API
import com.mtdevelopment.admin.data.model.GoogleComputeBody
import com.mtdevelopment.admin.data.model.GoogleOptimizedComputedRouteResponse
import com.mtdevelopment.admin.data.model.RouteModifiers
import com.mtdevelopment.admin.data.model.VehicleInfo
import com.mtdevelopment.admin.data.model.WaypointData
import com.mtdevelopment.core.util.NetWorkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.serialization.json.Json

class GoogleRouteDataSource(
    private val client: HttpClient,
    private val json: Json
) {

    suspend fun getOptimizedRoute(addressesList: List<String>): NetWorkResult<GoogleOptimizedComputedRouteResponse> {

        val intermediatesWaypointsList = addressesList.map {
            WaypointData(
                address = it,
                via = false
            )
        }

        val response = try {
            client.post {
                url {
                    path(
                        "directions/v2:computeRoutes"
                    )
                    contentType(ContentType.Application.Json)
                    header(
                        "X-Goog-Api-Key",
                        GOOGLE_API
                    )
                    header(
                        "X-Goog-FieldMask",
                        "routes.legs.endLocation,routes.optimized_intermediate_waypoint_index"
                    )
                    setBody(
                        GoogleComputeBody(
                            origin = WaypointData(
                                address = "8 La Vessoye, 25560 Boujailles",
                                via = false
                            ),
                            destination = WaypointData(
                                address = "8 La Vessoye, 25560 Boujailles",
                                via = false
                            ),
                            intermediates = intermediatesWaypointsList,
                            travelMode = "DRIVE",
                            routingPreference = "TRAFFIC_AWARE",
                            polylineQuality = "HIGH_QUALITY",
                            polylineEncoding = "ENCODED_POLYLINE",
                            computeAlternativeRoutes = false,
                            routeModifiers = RouteModifiers(
                                avoidHighways = false,
                                avoidFerries = true,
                                avoidTolls = true,
                                vehicleInfo = VehicleInfo(
                                    emissionType = "DIESEL"
                                )
                            ),
                            languageCode = "fr-FR",
                            units = "METRIC",
                            optimizeWaypointOrder = true
                        )
                    )
                    expectSuccess = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return NetWorkResult.Error("", e.message ?: "")
        }
        return try {
            val result =
                json.decodeFromString<GoogleOptimizedComputedRouteResponse>(response.body())
            NetWorkResult.Success(
                result
            )
        } catch (e: Exception) {
            NetWorkResult.Error(response.status.toString(), e.message ?: "")
        }
    }

}