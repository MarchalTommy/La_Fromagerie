package com.mtdevelopment.delivery.data.source.remote

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.mtdevelopment.core.model.DeliveryCity
import com.mtdevelopment.delivery.data.model.response.firestore.DataDeliveryPathsResponse
import com.mtdevelopment.delivery.data.model.response.firestore.toDeliveryCities
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class FirestoreDeliveryDataSourceTest {

    private val firestore: FirebaseFirestore = mockk()
    private val collection: CollectionReference = mockk()
    private val query: Query = mockk()
    private val task: Task<QuerySnapshot> = mockk(relaxed = true)

    private val dataSource = FirestoreDeliveryDataSource(firestore)

    @Test
    fun `getDeliveryPath queries the stored path_name field, not pathName`() {
        val fieldSlot = slot<String>()
        every { firestore.collection("delivery_paths") } returns collection
        every { collection.whereEqualTo(capture(fieldSlot), any<String>()) } returns query
        every { query.get() } returns task

        dataSource.getDeliveryPath(
            pathName = "Vercors",
            onSuccess = {},
            onFailure = {}
        )

        // Regression guard for the latent bug: the stored Firestore field is `path_name`
        // (see getAllDeliveryPaths / the admin write DTO), NOT `pathName`. Querying the
        // wrong key silently matched nothing.
        verify { collection.whereEqualTo("path_name", "Vercors") }
        assertEquals("path_name", fieldSlot.captured)
    }

    @Test
    fun `getDeliveryPath maps the first matching document to a response`() {
        every { firestore.collection("delivery_paths") } returns collection
        every { collection.whereEqualTo(any<String>(), any<String>()) } returns query
        every { query.get() } returns task

        val successSlot = slot<OnSuccessListener<QuerySnapshot>>()
        every { task.addOnFailureListener(any()) } returns task
        every { task.addOnSuccessListener(capture(successSlot)) } returns task

        val document: DocumentSnapshot = mockk()
        every { document.id } returns "path-1"
        every { document.data } returns mapOf(
            "path_name" to "Vercors",
            "delivery_day" to "MONDAY",
            "city_entries" to listOf(
                mapOf(
                    "city" to "Grenoble",
                    // Firestore hands integers back as Long — the mapper must not cast to Int.
                    "postcode" to 38000L,
                    "streets" to listOf("Rue de la Fromagerie")
                )
            )
        )
        val snapshot: QuerySnapshot = mockk()
        every { snapshot.documents } returns listOf(document)

        var result: DataDeliveryPathsResponse? = null
        dataSource.getDeliveryPath(
            pathName = "Vercors",
            onSuccess = { result = it },
            onFailure = { result = null }
        )

        successSlot.captured.onSuccess(snapshot)

        assertEquals("Vercors", result?.path_name)
        assertEquals(
            listOf(
                DeliveryCity("Grenoble", 38000, listOf("Rue de la Fromagerie"))
            ),
            result?.toDeliveryCities()
        )
    }

    @Test
    fun `getDeliveryPath reads the legacy parallel arrays when city_entries is absent`() {
        every { firestore.collection("delivery_paths") } returns collection
        every { collection.whereEqualTo(any<String>(), any<String>()) } returns query
        every { query.get() } returns task

        val successSlot = slot<OnSuccessListener<QuerySnapshot>>()
        every { task.addOnFailureListener(any()) } returns task
        every { task.addOnSuccessListener(capture(successSlot)) } returns task

        val document: DocumentSnapshot = mockk()
        every { document.id } returns "path-1"
        every { document.data } returns mapOf(
            "path_name" to "Vercors",
            "cities" to listOf("Grenoble"),
            "delivery_day" to "MONDAY",
            "postcodes" to listOf(38000L)
        )
        val snapshot: QuerySnapshot = mockk()
        every { snapshot.documents } returns listOf(document)

        var result: DataDeliveryPathsResponse? = null
        dataSource.getDeliveryPath(
            pathName = "Vercors",
            onSuccess = { result = it },
            onFailure = { result = null }
        )

        successSlot.captured.onSuccess(snapshot)

        assertEquals(listOf(DeliveryCity("Grenoble", 38000)), result?.toDeliveryCities())
    }

    @Test
    fun `getDeliveryPath fails instead of crashing when no document matches`() {
        every { firestore.collection("delivery_paths") } returns collection
        every { collection.whereEqualTo(any<String>(), any<String>()) } returns query
        every { query.get() } returns task

        val successSlot = slot<OnSuccessListener<QuerySnapshot>>()
        every { task.addOnFailureListener(any()) } returns task
        every { task.addOnSuccessListener(capture(successSlot)) } returns task

        val snapshot: QuerySnapshot = mockk()
        every { snapshot.documents } returns emptyList()

        var failed = false
        dataSource.getDeliveryPath(
            pathName = "Inconnu",
            onSuccess = {},
            onFailure = { failed = true }
        )

        successSlot.captured.onSuccess(snapshot)

        assertEquals(true, failed)
    }
}
