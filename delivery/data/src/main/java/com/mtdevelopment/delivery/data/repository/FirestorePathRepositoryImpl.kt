package com.mtdevelopment.delivery.data.repository

import com.mtdevelopment.core.util.NetWorkResult
import com.mtdevelopment.delivery.data.model.response.firestore.toDeliveryCities
import com.mtdevelopment.delivery.data.source.remote.FirestoreDeliveryDataSource
import com.mtdevelopment.delivery.data.source.remote.OpenRouteDataSource
import com.mtdevelopment.delivery.domain.model.DeliveryPath
import com.mtdevelopment.delivery.domain.repository.AddressApiRepository
import com.mtdevelopment.delivery.domain.repository.PathFetchOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * Implementation of [FirestorePathRepository] that reconstructs a full [DeliveryPath]
 * by combining data from multiple sources:
 * 1. Firestore: Primary path metadata (names, cities, postcodes).
 * 2. Address API: Translates city/zip into geographic center coordinates.
 * 3. OpenRouteService: Provides the road-based GeoJSON geometry between city centers.
 */
class FirestorePathRepositoryImpl(
    private val firestore: FirestoreDeliveryDataSource,
    private val openRouteService: OpenRouteDataSource,
    private val addressApiRepository: AddressApiRepository
) : com.mtdevelopment.delivery.domain.repository.FirestorePathRepository {

    /**
     * Fetches all delivery paths and enriches them with geographic data.
     *
     * Orchestration Logic:
     * 1. Fetches raw path data from Firestore.
     * 2. For each path, launches asynchronous geocoding requests for all covered cities.
     * 3. Awaits geocoding results to establish the geographic center of the path.
     * 4. If [withGeoJson] is true, fetches the driving route (geometry) from OpenRouteService.
     * 5. Drops any path where a city failed to geocode — but reports it, see below.
     *
     * A dropped path is **not** the same fact as a deleted path, so the two travel separately in
     * [PathFetchOutcome]: `remoteIds` carries every document Firestore listed, and `degraded` says
     * the picture is incomplete. Enrichment is all-or-nothing per path on purpose:
     * [DeliveryPath.locations] is positionally aligned with `cities` and
     * `DetermineDeliveryEligibilityUseCase` indexes one by the other, so a path missing one city's
     * coordinate would silently attribute the wrong center to every city after it.
     */
    override fun getAllDeliveryPaths(
        withGeoJson: Boolean,
        onSuccess: (PathFetchOutcome) -> Unit,
        onFailure: () -> Unit
    ) {
        firestore.getAllDeliveryPaths(onSuccess = { pathList ->
            CoroutineScope(Dispatchers.IO).launch {
                // Captured before any filtering: this is the "does the shop still have this
                // tournée" answer, and the only thing the cache cleanup may act on.
                val remoteIds = pathList.map { it.id }.toSet()
                // Prepare data for reverse geocoding. Cities come from `city_entries` when present,
                // otherwise from the legacy parallel arrays (see DataDeliveryPathsResponse).
                val pathsWithCities = pathList.mapNotNull { path ->
                    path.toDeliveryCities().takeIf { it.isNotEmpty() }?.let { path to it }
                }

                val deferredCityInfoList = pathsWithCities.map { (path, deliveryCities) ->
                    // Launch async calls for reverse geocoding in parallel for efficiency
                    val deferredCities = deliveryCities.map { city ->
                        async {
                            addressApiRepository.reverseGeocodeCity(
                                name = city.name,
                                zip = city.postcode
                            )
                        }
                    }
                    // Associate necessary info for final reconstruction
                    Triple(path, deliveryCities, deferredCities)
                }

                // Await geocoding results and build DeliveryPaths
                val finalPaths =
                    deferredCityInfoList.mapNotNull { (pathData, deliveryCities, deferredCities) ->
                        // Await resolution of all geocoding requests for this path
                        val cityInfos = deferredCities.map { it.await() }

                        // Check if all city information was retrieved
                        if (cityInfos.any { it == null }) {
                            // If info is missing, ignore this specific path by returning null
                            null
                        } else {
                            // Calculate the list of locations (latitude, longitude)
                            val locations = cityInfos.mapNotNull { cityInfo ->
                                cityInfo?.location?.let { Pair(it.latitude, it.longitude) }
                            }

                            // Get GeoJson only if requested and locations are available.
                            // A missing road line is a degraded rendering, not a failed load: the
                            // path is perfectly usable without it, only its polyline is absent. It
                            // used to call onFailure here AND still return the path through
                            // onSuccess, so both callbacks fired and the caller raced itself into
                            // showing an error over a correctly loaded list.
                            val geoJsonData = if (withGeoJson && locations.isNotEmpty()) {
                                val result = openRouteService.getGeoJsonForLngLatList(locations)
                                (result as? NetWorkResult.Success)?.data
                            } else {
                                null // No GeoJson requested or no locations
                            }

                            // Build the final DeliveryPath object
                            DeliveryPath(
                                id = pathData.id,
                                pathName = pathData.path_name ?: "",
                                cities = deliveryCities,
                                locations = locations,
                                deliveryDay = pathData.deliveryDay,
                                deliveryFrequency = pathData.deliveryFrequency,
                                geoJson = geoJsonData
                            )
                        }
                    }

                // An empty result is ALWAYS a failure, never a legitimate "nothing is
                // deliverable" answer: the shop always has at least one path. Three ways to
                // land here, and all of them used to be reported as success:
                //  - Firestore returned no document at all. Offline, `get()` falls back to
                //    the local Firestore cache and COMPLETES SUCCESSFULLY with zero
                //    documents, so the failure listener never fires.
                //  - Every path was dropped above because a city failed to geocode.
                //  - Every path resolved to zero cities, i.e. no document carried either
                //    `city_entries` or the legacy `cities`/`postcodes` arrays.
                // Calling onSuccess with an empty list makes the caller mark the cache as
                // synchronized and wipe it, permanently leaving the customer with no
                // deliverable address. See GetAllDeliveryPathsUseCase.
                if (finalPaths.isEmpty()) {
                    onFailure.invoke()
                } else {
                    onSuccess.invoke(
                        PathFetchOutcome(
                            paths = finalPaths,
                            remoteIds = remoteIds,
                            // Anything Firestore listed but we could not rebuild — a city that
                            // failed to geocode, or a document carrying no city at all.
                            degraded = finalPaths.size < remoteIds.size
                        )
                    )
                }
            }
        }, onFailure = onFailure)
    }

    /**
     * Fetches a single delivery path by name (partial reconstruction without geographic enrichment).
     */
    override fun getDeliveryPath(
        pathName: String,
        onSuccess: (DeliveryPath?) -> Unit,
        onFailure: () -> Unit
    ) {
        firestore.getDeliveryPath(
            pathName = pathName,
            onSuccess = { path ->
                val deliveryCities = path.toDeliveryCities()

                if (path.path_name?.isNotBlank() == true && path.id.isNotBlank() && deliveryCities.isNotEmpty()) {
                    onSuccess.invoke(
                        DeliveryPath(
                            id = path.id,
                            pathName = path.path_name,
                            cities = deliveryCities,
                            geoJson = null,
                            deliveryDay = path.deliveryDay,
                            deliveryFrequency = path.deliveryFrequency,
                            locations = null
                        )
                    )
                } else {
                    onFailure.invoke()
                }
            },
            onFailure = onFailure
        )
    }


}