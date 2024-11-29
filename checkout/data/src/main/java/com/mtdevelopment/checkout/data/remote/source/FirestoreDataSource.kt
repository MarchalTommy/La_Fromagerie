package com.mtdevelopment.checkout.data.remote.source

import com.google.firebase.firestore.FirebaseFirestore
import com.mtdevelopment.checkout.data.remote.model.response.firestore.DataDeliveryPathsResponse

class FirestoreDataSource(
    private val firestore: FirebaseFirestore
) {
    fun getAllDeliveryPaths(onSuccess: (List<DataDeliveryPathsResponse>) -> Unit, onFailure: () -> Unit) {
        firestore.collection("delivery_paths")
            .get()
            .addOnFailureListener {
                onFailure.invoke()
            }
            .addOnSuccessListener {
                onSuccess.invoke(it.documents.map { item ->
                    DataDeliveryPathsResponse(
                        pathName = item.data?.get("path_name").toString(),
                        availableCities = item.data?.get("cities") as? List<String>
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
                    it.documents[0].toObject(DataDeliveryPathsResponse::class.java)
                        ?: DataDeliveryPathsResponse()
                )
            }
    }

}