package com.mtdevelopment.delivery.data.source.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.mtdevelopment.delivery.data.model.response.firestore.DataDeliveryCityResponse
import com.mtdevelopment.delivery.data.model.response.firestore.DataDeliveryPathsResponse
import com.mtdevelopment.delivery.data.model.response.firestore.DataPickupPointResponse
import com.mtdevelopment.delivery.data.model.response.firestore.toPickupPointResponse

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

    /**
     * Reads every pickup point for the customer journey.
     *
     * ⚠️ Zero documents means two opposite things, and [QuerySnapshot.getMetadata] is what
     * tells them apart. Firestore serves reads from its own cache when offline and completes
     * them *successfully* with zero documents, so `addOnFailureListener` never fires — taking
     * that at face value is the bug that once made every address undeliverable on a first
     * offline launch. Served from cache and empty is therefore a failed read.
     *
     * Served by the **server** and empty is the truth: nothing is configured. That is not a
     * hypothetical — it is the state of the collection until the shop's own counter is created
     * in the admin, so on the day this ships the first customer to tap "Boutique" would
     * otherwise be told to check their connection. This used to assume the shop always has at
     * least its counter configured; it does not, and the assumption belonged to no one but
     * this comment.
     */
    fun getAllPickupPoints(
        onSuccess: (List<DataPickupPointResponse>) -> Unit,
        onFailure: () -> Unit
    ) {
        firestore.collection("pickup_points")
            .get()
            .addOnFailureListener {
                onFailure.invoke()
            }
            .addOnSuccessListener { snapshot ->
                val points = snapshot.documents.map { item ->
                    item.data.toPickupPointResponse(item.id)
                }
                if (points.isEmpty() && snapshot.metadata.isFromCache) {
                    onFailure.invoke()
                } else {
                    onSuccess.invoke(points)
                }
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
