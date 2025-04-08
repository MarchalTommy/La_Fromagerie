package com.mtdevelopment.delivery.data.source.remote

import com.google.firebase.firestore.FirebaseFirestore

class FirestoreDataSource(
    private val firestore: FirebaseFirestore
) {
    fun getAllDeliveryPaths(
        onSuccess: (List<com.mtdevelopment.delivery.data.model.response.firestore.DataDeliveryPathsResponse>) -> Unit,
        onFailure: () -> Unit
    ) {
        firestore.collection("delivery_paths")
            .get()
            .addOnFailureListener {
                onFailure.invoke()
            }
            .addOnSuccessListener {
                onSuccess.invoke(it.documents.map { item ->
                    com.mtdevelopment.delivery.data.model.response.firestore.DataDeliveryPathsResponse(
                        id = item.id,
                        path_name = item.data?.get("path_name").toString(),
                        cities = item.data?.get("cities") as? List<String>,
                        deliveryDay = item.data?.get("delivery_day").toString(),
                        postcodes = item.data?.get("postcodes") as? List<Int>
                            ?: emptyList()
                    )
                })
            }
    }

    fun getDeliveryPath(
        pathName: String,
        onSuccess: (com.mtdevelopment.delivery.data.model.response.firestore.DataDeliveryPathsResponse) -> Unit,
        onFailure: () -> Unit
    ) {
        firestore.collection("delivery_paths")
            .whereEqualTo("pathName", pathName)
            .get()
            .addOnFailureListener {
                onFailure.invoke()
            }
            .addOnSuccessListener {
                onSuccess.invoke(
                    com.mtdevelopment.delivery.data.model.response.firestore.DataDeliveryPathsResponse(
                        id = it.documents[0].id,
                        path_name = it.documents[0].data?.get("path_name").toString(),
                        cities = it.documents[0].data?.get("cities") as? List<String>,
                        deliveryDay = it.documents[0].data?.get("delivery_day").toString(),
                        postcodes = it.documents[0].data?.get("postcodes") as? List<Int>
                            ?: emptyList()
                    )
                )
            }
    }

}