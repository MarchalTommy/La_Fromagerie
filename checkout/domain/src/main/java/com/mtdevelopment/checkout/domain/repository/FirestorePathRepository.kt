package com.mtdevelopment.checkout.domain.repository

import com.mtdevelopment.checkout.domain.model.DeliveryPath

interface FirestorePathRepository {

    fun getAllDeliveryPaths(
        onSuccess: (List<DeliveryPath?>) -> Unit,
        onFailure: () -> Unit
    )

    fun getDeliveryPath(
        pathName: String,
        onSuccess: (DeliveryPath?) -> Unit,
        onFailure: () -> Unit
    )

}