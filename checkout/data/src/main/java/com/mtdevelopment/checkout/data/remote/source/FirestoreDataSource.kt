package com.mtdevelopment.checkout.data.remote.source

import com.google.firebase.firestore.FirebaseFirestore
import com.mtdevelopment.checkout.data.remote.model.response.firestore.DataDeliveryPathsResponse

class FirestoreDataSource(
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
                    DataDeliveryPathsResponse(
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
        onSuccess: (DataDeliveryPathsResponse) -> Unit,
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
                    DataDeliveryPathsResponse(
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