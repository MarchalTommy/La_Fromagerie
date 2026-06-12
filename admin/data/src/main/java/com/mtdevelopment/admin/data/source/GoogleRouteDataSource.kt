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

/**
 * Data source for interacting with Google Maps Directions API v2.
 * It calculates optimized routes for a list of addresses.
 */
class GoogleRouteDataSource(
    private val client: HttpClient,
    private val json: Json
) {

    private companion object {
        /**
         * The farm's address ("La Fromagerie"), used as both origin and destination of every
         * optimized delivery route.
         */
        const val HOME_BASE_ADDRESS = "8 La Vessoye, 25560 Boujailles"
    }

    /**
     * Calls the Google Directions v2:computeRoutes endpoint to get an optimized route.
     * @param addressesList The list of intermediate stops.
     * @return A [NetWorkResult] containing the optimized route response.
     */
    suspend fun getOptimizedRoute(addressesList: List<String>): NetWorkResult<GoogleOptimizedComputedRouteResponse> {

        // Transform address strings into WaypointData objects
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
                    // Authentication via API Key from BuildConfig
                    header(
                        "X-Goog-Api-Key",
                        GOOGLE_API
                    )
                    // Crucial: The FieldMask determines which fields are returned in the response.
                    // We need the legs (for coordinates) and the optimized index (for reordering).
                    header(
                        "X-Goog-FieldMask",
                        "routes.legs.endLocation,routes.optimized_intermediate_waypoint_index"
                    )
                    setBody(
                        GoogleComputeBody(
                            // Routes always start and end at the farm: deliveries are round trips
                            // from La Fromagerie. (flagged for review: making this configurable or
                            // based on the admin's live position is a product decision.)
                            origin = WaypointData(
                                address = HOME_BASE_ADDRESS,
                                via = false
                            ),
                            destination = WaypointData(
                                address = HOME_BASE_ADDRESS,
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
                                    emissionType = "DIESEL" // Assuming a diesel delivery vehicle
                                )
                            ),
                            languageCode = "fr-FR",
                            units = "METRIC",
                            optimizeWaypointOrder = true // Ask Google to find the best order for intermediate stops
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
            // Decoding the JSON response into our model
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