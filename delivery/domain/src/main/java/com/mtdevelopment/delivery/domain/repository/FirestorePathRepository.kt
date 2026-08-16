package com.mtdevelopment.delivery.domain.repository

import com.mtdevelopment.core.model.PickupPoint
import com.mtdevelopment.delivery.domain.model.DeliveryPath

interface FirestorePathRepository {

    fun getAllDeliveryPaths(
        withGeoJson: Boolean = false,
        onSuccess: (List<DeliveryPath?>) -> Unit,
        onFailure: () -> Unit
    )

    fun getDeliveryPath(
        pathName: String,
        onSuccess: (DeliveryPath?) -> Unit,
        onFailure: () -> Unit
    )

    /**
     * Fetches the places an order can be collected at.
     *
     * Read straight from Firestore on each visit rather than through a Room cache: there is a
     * handful of these documents, the screen that asks for them already needs the network for
     * address matching, and a cache with no offline story to tell would only add a way to serve
     * a market date that has since been cancelled.
     *
     * [onFailure] also covers an empty read — see the data source for why zero documents is
     * never an honest answer here.
     */
    fun getAllPickupPoints(
        onSuccess: (List<PickupPoint>) -> Unit,
        onFailure: () -> Unit
    )

}