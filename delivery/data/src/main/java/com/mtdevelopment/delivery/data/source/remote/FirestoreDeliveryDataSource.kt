package com.mtdevelopment.delivery.data.source.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.mtdevelopment.delivery.data.model.response.firestore.DataDeliveryCityResponse
import com.mtdevelopment.delivery.data.model.response.firestore.DataDeliveryPathsResponse

class FirestoreDeliveryDataSource(
    private val firestore: FirebaseFirestore
) {
    fun getAllDeliveryPaths(
        onSuccess: (List<DataDeliveryPathsResponse>) -> Unit,
        onFailure: () -> Unit
    ) {
        firestore.collection("delivery_paths")
            .get()
            .addOnFailureListener {
                onFailure.invoke()
            }
            .addOnSuccessListener {
                onSuccess.invoke(it.documents.map { item ->
                    item.data.toPathResponse(item.id)
                })
            }
    }

    fun getDeliveryPath(
        pathName: String,
        onSuccess: (DataDeliveryPathsResponse) -> Unit,
        onFailure: () -> Unit
    ) {
        firestore.collection("delivery_paths")
            .whereEqualTo("path_name", pathName)
            .get()
            .addOnFailureListener {
                onFailure.invoke()
            }
            .addOnSuccessListener {
                val document = it.documents.firstOrNull()
                if (document == null) {
                    onFailure.invoke()
                } else {
                    onSuccess.invoke(document.data.toPathResponse(document.id))
                }
            }
    }
}

/**
 * Maps a raw Firestore document map to the read DTO.
 *
 * Numbers are read through [Number] rather than cast straight to `Int`: Firestore always hands
 * back integers as `Long`, and `as? List<Int>` is erased at runtime, so the bad cast only blows up
 * later at the point of use.
 */
private fun Map<String, Any?>?.toPathResponse(documentId: String): DataDeliveryPathsResponse =
    DataDeliveryPathsResponse(
        id = documentId,
        path_name = this?.get("path_name")?.toString(),
        cities = (this?.get("cities") as? List<*>)?.mapNotNull { it?.toString() },
        deliveryDay = this?.get("delivery_day")?.toString() ?: "",
        deliveryFrequency = this?.get("delivery_frequency")?.toString() ?: "WEEKLY",
        postcodes = (this?.get("postcodes") as? List<*>)?.mapNotNull { (it as? Number)?.toInt() },
        cityEntries = (this?.get("city_entries") as? List<*>)?.mapNotNull { entry ->
            val fields = entry as? Map<*, *> ?: return@mapNotNull null
            val city = fields["city"]?.toString() ?: return@mapNotNull null
            DataDeliveryCityResponse(
                city = city,
                postcode = (fields["postcode"] as? Number)?.toInt() ?: 0,
                streets = (fields["streets"] as? List<*>)?.mapNotNull { it?.toString() }
                    ?: emptyList()
            )
        }
    )
