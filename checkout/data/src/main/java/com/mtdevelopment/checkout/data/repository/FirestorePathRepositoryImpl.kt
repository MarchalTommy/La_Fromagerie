package com.mtdevelopment.checkout.data.repository

import com.mtdevelopment.checkout.data.remote.source.FirestoreDataSource
import com.mtdevelopment.checkout.data.remote.source.OpenRouteDataSource
import com.mtdevelopment.checkout.domain.model.DeliveryPath
import com.mtdevelopment.checkout.domain.repository.FirestorePathRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirestorePathRepositoryImpl(
    private val firestore: FirestoreDataSource,
    private val openRouteService: OpenRouteDataSource
) : FirestorePathRepository {
    override fun getAllDeliveryPaths(
        onSuccess: (List<DeliveryPath?>) -> Unit,
        onFailure: () -> Unit
    ) {
        /**
         * LET ME EXPLAIN !
         * First, we get paths from firestore (we don't know their geojson yet, cause we don't store them)
         */
        firestore.getAllDeliveryPaths(onSuccess = { pathList ->
            CoroutineScope(Dispatchers.IO).launch {
                /**
                 * Then, we create a list of async job to get geojson for each path
                 */
                val deferredPaths = listOf(async {
                    /**
                     * This part here is to get geoJson "synchronously", but not really, wait and see
                     */
                    suspendCoroutine { coroutine ->
                        /**
                         * Here we operate on each path
                         */
                        pathList.forEach { path ->
                            if (path.pathName?.isNotBlank() == true && path.availableCities != null) {
                                /**
                                 * We GET BACK in asynchronous mode to be able to call a ws and a GeoCoder
                                 */
                                CoroutineScope(Dispatchers.IO).launch {
                                    openRouteService.getLngLatForCities(path.availableCities) { geoJsonResult ->
                                        /**
                                         * We send the response back to the first scope, to be able to get them out of here
                                         */
                                        coroutine.resume(
                                            DeliveryPath(
                                                id = path.id,
                                                pathName = path.pathName,
                                                availableCities = path.availableCities,
                                                geoJson = geoJsonResult.data.toString()
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                })

                /**
                 * Finally we await for all those async job to be done
                 */
                val finalPaths = deferredPaths.awaitAll()
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
                if (path.pathName?.isNotBlank() == true && path.id.isNotBlank() && path.availableCities != null) {
                    onSuccess.invoke(
                        DeliveryPath(
                            id = path.id,
                            pathName = path.pathName,
                            availableCities = path.availableCities,
                            geoJson = ""
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