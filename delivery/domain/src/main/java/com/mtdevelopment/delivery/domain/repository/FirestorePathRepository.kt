package com.mtdevelopment.delivery.domain.repository

interface FirestorePathRepository {

    fun getAllDeliveryPaths(
        onSuccess: (List<com.mtdevelopment.delivery.domain.model.DeliveryPath?>) -> Unit,
        onFailure: () -> Unit
    )

    fun getDeliveryPath(
        pathName: String,
        onSuccess: (com.mtdevelopment.delivery.domain.model.DeliveryPath?) -> Unit,
        onFailure: () -> Unit
    )

}