package com.mtdevelopment.admin.data.model

import com.mtdevelopment.core.model.DeliveryCity
import com.mtdevelopment.core.model.DeliveryPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The write half of the Firestore contract: street restrictions entered by the admin have to
 * actually reach the document. Before the split-city work the DTO had no street field at all, so
 * everything typed in the admin dialog was dropped on save.
 */
class DataDeliveryPathTest {

    private val splitPath = DeliveryPath(
        id = "path-a",
        pathName = "Parcours A",
        availableCities = listOf(
            DeliveryCity("Boujailles", 25560, listOf("Rue du Moulin", "Grande Rue")),
            DeliveryCity("Frasne", 25560),
            DeliveryCity("Courvière", 25560)
        ),
        deliveryDay = "TUESDAY"
    )

    /**
     * The write side of the "reading a path needs no network" change: if the center does not reach
     * the document, every reader goes back to geocoding all of its cities on every load.
     */
    @Test
    fun `commune centers reach the Firestore DTO`() {
        val dto = DeliveryPath(
            id = "path-b",
            pathName = "Le Haut",
            availableCities = listOf(
                DeliveryCity("Malpas", 25160, emptyList(), 46.80, 6.29),
                DeliveryCity("Frasne", 25560)
            ),
            deliveryDay = "FRIDAY"
        ).toDataDeliveryPath()

        assertEquals(46.80, dto.city_entries?.get(0)?.lat)
        assertEquals(6.29, dto.city_entries?.get(0)?.lng)
        // A city that was never resolved is written without coordinates rather than with zeroes,
        // so the reader can tell "not resolved" from "resolved to the Gulf of Guinea".
        assertNull(dto.city_entries?.get(1)?.lat)
        assertNull(dto.city_entries?.get(1)?.lng)
    }

    @Test
    fun `street restrictions reach the Firestore DTO`() {
        val dto = splitPath.toDataDeliveryPath()

        assertEquals(
            listOf(
                DataDeliveryCity("Boujailles", 25560, listOf("Rue du Moulin", "Grande Rue")),
                DataDeliveryCity("Frasne", 25560),
                DataDeliveryCity("Courvière", 25560)
            ),
            dto.city_entries
        )
    }

    /**
     * `cities`/`postcodes` are a one-way projection of `city_entries`, kept so a client build
     * reading only the old shape still sees the coverage. They must stay aligned and in order —
     * they are zipped positionally on the read side.
     */
    @Test
    fun `legacy parallel arrays are derived from the city entries in order`() {
        val dto = splitPath.toDataDeliveryPath()

        assertEquals(listOf("Boujailles", "Frasne", "Courvière"), dto.cities)
        assertEquals(listOf(25560, 25560, 25560), dto.postcodes)
        assertEquals(dto.city_entries?.size, dto.cities?.size)
    }

    @Test
    fun `path identity and schedule are carried over`() {
        val dto = splitPath.toDataDeliveryPath()

        assertEquals("path-a", dto.id)
        assertEquals("Parcours A", dto.path_name)
        assertEquals("TUESDAY", dto.delivery_day)
        assertEquals("WEEKLY", dto.delivery_frequency)
    }

    /**
     * The path-level `streets` field is deliberately gone: an older client applies it across every
     * city of the path, which would make the unrestricted cities of a split path undeliverable
     * there. Its absence is the intended degradation — those clients see every city as fully
     * covered instead.
     */
    @Test
    fun `no path-level streets field is written`() {
        val fieldNames = DataDeliveryPath::class.java.declaredFields.map { it.name }

        assertNull(fieldNames.firstOrNull { it == "streets" })
    }
}
