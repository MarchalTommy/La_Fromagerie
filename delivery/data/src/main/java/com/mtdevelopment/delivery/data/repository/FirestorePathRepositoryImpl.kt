package com.mtdevelopment.delivery.data.repository

import com.mtdevelopment.core.util.NetWorkResult
import com.mtdevelopment.delivery.data.source.remote.FirestoreDeliveryDataSource
import com.mtdevelopment.delivery.data.source.remote.OpenRouteDataSource
import com.mtdevelopment.delivery.domain.model.DeliveryPath
import com.mtdevelopment.delivery.domain.repository.AddressApiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class FirestorePathRepositoryImpl(
    private val firestore: FirestoreDeliveryDataSource,
    private val openRouteService: OpenRouteDataSource,
    private val addressApiRepository: AddressApiRepository
) : com.mtdevelopment.delivery.domain.repository.FirestorePathRepository {

    override fun getAllDeliveryPaths(
        withGeoJson: Boolean,
        onSuccess: (List<DeliveryPath?>) -> Unit,
        onFailure: () -> Unit
    ) {
        /**
         * LET ME EXPLAIN !
         * First, we get paths from firestore
         */
        firestore.getAllDeliveryPaths(onSuccess = { pathList ->
            CoroutineScope(Dispatchers.IO).launch {
                // Prepare data for reverse geocoding
                val pathsWithCities = pathList.filter {
                    it.cities?.isNotEmpty() == true && it.postcodes?.isNotEmpty() == true
                }

                val deferredCityInfoList = pathsWithCities.map { path ->
                    val zippedCities = path.cities!! zip path.postcodes!!
                    // Launch async calls for reverse geocoding
                    val deferredCities = zippedCities.map { cityPair ->
                        async {
                            addressApiRepository.reverseGeocodeCity(
                                name = cityPair.first,
                                zip = cityPair.second
                            )
                        }
                    }
                    // Associate necessary info for final reconstruction
                    Triple(path, zippedCities, deferredCities)
                }

                // Await geocoding results and build DeliveryPaths
                val finalPaths =
                    deferredCityInfoList.mapNotNull { (pathData, zippedCities, deferredCities) ->
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

                            // Get GeoJson only if requested and locations are available
                            val geoJsonData = if (withGeoJson && locations.isNotEmpty()) {
                                val result = openRouteService.getGeoJsonForLngLatList(locations)

                                if (result is NetWorkResult.Error) {
                                    onFailure.invoke()
                                    null
                                } else {
                                    (result as? NetWorkResult.Success)?.data
                                }

                            } else {
                                null // No GeoJson requested or no locations
                            }

                            // Build the final DeliveryPath object
                            DeliveryPath(
                                id = pathData.id,
                                pathName = pathData.path_name ?: "",
                                availableCities = zippedCities,
                                locations = locations,
                                deliveryDay = pathData.deliveryDay,
                                geoJson = geoJsonData // Use null if withGeoJson is false
                            )
                        }
                    }

                // Check if paths were skipped due to geocoding errors
                if (finalPaths.size != deferredCityInfoList.size && finalPaths.isEmpty()) {
                    // If all paths failed, call onFailure
                    onFailure.invoke()
                } else {
                    // Otherwise, return the successful paths
                    onSuccess.invoke(finalPaths)
                }
            }
        }, onFailure = onFailure)
    }

    override fun getDeliveryPath(
        pathName: String,
        onSuccess: (DeliveryPath?) -> Unit,
        onFailure: () -> Unit
    ) {
        firestore.getDeliveryPath(
            pathName = pathName,
            onSuccess = { path ->

                val listOfZipped = mutableListOf<List<Pair<String, Int>>>()
                if (path.cities?.isNotEmpty() == true && path.postcodes?.isNotEmpty() == true) {
                    listOfZipped.add(path.cities zip path.postcodes)
                }

                if (path.path_name?.isNotBlank() == true && path.id.isNotBlank() && path.cities != null) {
                    onSuccess.invoke(
                        DeliveryPath(
                            id = path.id,
                            pathName = path.path_name,
                            availableCities = listOfZipped[0],
                            geoJson = null,
                            deliveryDay = "",
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