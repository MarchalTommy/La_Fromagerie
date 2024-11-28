package com.mtdevelopment.checkout.data.repository

import com.mtdevelopment.checkout.data.remote.source.FirestoreDataSource
import com.mtdevelopment.checkout.domain.model.DeliveryPath
import com.mtdevelopment.checkout.domain.repository.FirestorePathRepository

class FirestorePathRepositoryImpl(
    private val firestore: FirestoreDataSource
) : FirestorePathRepository {
    override fun getAllDeliveryPaths(
        onSuccess: (List<DeliveryPath?>) -> Unit,
        onFailure: () -> Unit
    ) {
        firestore.getAllDeliveryPaths(onSuccess = { pathList ->
            onSuccess.invoke(pathList.map { path ->
                if (path.pathName?.isNotBlank() == true && path.id.isNotBlank() && path.availableCities != null) {
                    DeliveryPath(
                        id = path.id,
                        pathName = path.pathName,
                        availableCities = path.availableCities,
                        geoJson = ""
                    )
                } else {
                    null
                }
            })
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