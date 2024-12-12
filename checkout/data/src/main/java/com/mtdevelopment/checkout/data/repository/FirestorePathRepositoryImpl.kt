package com.mtdevelopment.checkout.data.repository

import com.mtdevelopment.checkout.data.remote.source.FirestoreDataSource
import com.mtdevelopment.checkout.data.remote.source.OpenRouteDataSource
import com.mtdevelopment.checkout.domain.model.CityInformation
import com.mtdevelopment.checkout.domain.model.DeliveryPath
import com.mtdevelopment.checkout.domain.repository.AddressApiRepository
import com.mtdevelopment.checkout.domain.repository.FirestorePathRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class FirestorePathRepositoryImpl(
    private val firestore: FirestoreDataSource,
    private val openRouteService: OpenRouteDataSource,
    private val addressApiRepository: AddressApiRepository
) : FirestorePathRepository {

    override fun getAllDeliveryPaths(
        onSuccess: (List<DeliveryPath?>) -> Unit,
        onFailure: () -> Unit
    ) {
        /**
         * LET ME EXPLAIN !
         * First, we get paths from firestore
         */
        firestore.getAllDeliveryPaths(onSuccess = { pathList ->
            CoroutineScope(Dispatchers.IO).launch {

                val idList = pathList.map { it.id }

                val listOfZipped = mutableListOf<List<Pair<String, Int>>>()

                /**
                 * We zip their cities with their cities-postcodes to have pairs
                 */
                pathList.forEach { path ->
                    if (path.cities?.isNotEmpty() == true && path.postcodes?.isNotEmpty() == true) {
                        listOfZipped.add(path.cities zip path.postcodes)
                    }
                }

                /**
                 * We call the reverse geocoding API
                 */
                val deferredCityInfoList = mutableListOf<List<Deferred<CityInformation?>>>()
                listOfZipped.forEach { zipped ->
                    deferredCityInfoList.add(zipped.map {
                        async {
                            addressApiRepository.reverseGeocodeCity(
                                name = it.first,
                                zip = it.second
                            )
                        }
                    })
                }

                /**
                 * We use their locations from reverse geocoding to call for the geoJson API
                 */
                val locationsList = mutableListOf<List<Pair<Double, Double>>>()
                val geoJsons = deferredCityInfoList.map { deferred ->
                    val cities = deferred.map { it.await() }

                    locationsList.add(cities.mapNotNull {
                        Pair(
                            it?.location?.latitude ?: 0.0,
                            it?.location?.longitude ?: 0.0
                        )
                    })

                    openRouteService.getGeoJsonForLngLatList(cities.map {
                        Pair(
                            it?.location?.latitude ?: 0.0,
                            it?.location?.longitude ?: 0.0
                        )
                    }).data
                }


                val finalPaths = geoJsons.map { geoJson ->
                    DeliveryPath(
                        id = idList[geoJsons.indexOf(geoJson)],
                        pathName = pathList[geoJsons.indexOf(geoJson)].path_name ?: "",
                        availableCities = pathList[geoJsons.indexOf(geoJson)].cities
                            ?: listOf(),
                        locations = locationsList[geoJsons.indexOf(geoJson)],
                        deliveryDay = pathList[geoJsons.indexOf(geoJson)].deliveryDay,
                        geoJson = geoJson
                    )
                }

                onSuccess.invoke(finalPaths)

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
                if (path.path_name?.isNotBlank() == true && path.id.isNotBlank() && path.cities != null) {
                    onSuccess.invoke(
                        DeliveryPath(
                            id = path.id,
                            pathName = path.path_name,
                            availableCities = path.cities,
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