package com.mtdevelopment.delivery.data.repository

import com.mtdevelopment.delivery.data.source.remote.FirestoreDataSource
import com.mtdevelopment.delivery.data.source.remote.OpenRouteDataSource
import com.mtdevelopment.delivery.domain.model.CityInformation
import com.mtdevelopment.delivery.domain.model.DeliveryPath
import com.mtdevelopment.delivery.domain.repository.AddressApiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class FirestorePathRepositoryImpl(
    private val firestore: FirestoreDataSource,
    private val openRouteService: OpenRouteDataSource,
    private val addressApiRepository: AddressApiRepository
) : com.mtdevelopment.delivery.domain.repository.FirestorePathRepository {

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
                            val result = addressApiRepository.reverseGeocodeCity(
                                name = it.first,
                                zip = it.second
                            )

                            if (result == null) {
                                onFailure.invoke()
                                null
                            } else {
                                result
                            }
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
                        availableCities = listOfZipped[geoJsons.indexOf(geoJson)],
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