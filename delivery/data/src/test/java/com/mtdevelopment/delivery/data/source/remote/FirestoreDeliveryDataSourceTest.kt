package com.mtdevelopment.delivery.data.source.remote

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
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
            "cities" to listOf("Grenoble"),
            "delivery_day" to "MONDAY",
            "postcodes" to listOf(38000),
            "streets" to listOf("Rue de la Fromagerie")
        )
        val snapshot: QuerySnapshot = mockk()
        every { snapshot.documents } returns listOf(document)

        var result = mutableListOf<String?>()
        dataSource.getDeliveryPath(
            pathName = "Vercors",
            onSuccess = { result.add(it.path_name) },
            onFailure = { result.add(null) }
        )

        successSlot.captured.onSuccess(snapshot)

        assertEquals(listOf("Vercors"), result)
    }
}
