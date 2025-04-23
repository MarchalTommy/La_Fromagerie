package com.mtdevelopment.delivery.domain.repository

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

}