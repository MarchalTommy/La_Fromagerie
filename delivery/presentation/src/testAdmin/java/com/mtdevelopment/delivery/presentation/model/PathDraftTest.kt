package com.mtdevelopment.delivery.presentation.model

import com.mtdevelopment.admin.presentation.model.AdminUiDeliveryPath
import com.mtdevelopment.core.model.DeliveryCity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The path editor holds its draft in `rememberSaveable` rather than a ViewModel, so these reducers
 * are where the editing rules live and the only place they can be verified without Compose.
 */
class PathDraftTest {

    private val levier = DeliveryCity("Levier", 25270)
    private val boujailles = DeliveryCity("Boujailles", 25560, listOf("Rue du Moulin"))
    private val frasne = DeliveryCity("Frasne", 25560)

    private val draft = PathDraft(
        id = "path-a",
        name = "Parcours A",
        cities = listOf(levier, boujailles, frasne),
        deliveryDay = "TUESDAY",
        isNew = false
    )

    ///////////////////////////////////////////////////////////////////////////
    // Opening the editor
    ///////////////////////////////////////////////////////////////////////////

    /** A pre-filled name would have to be cleared before typing; the field shows a hint instead. */
    @Test
    fun `a null path opens an empty creation draft`() {
        val created = null.toDraft()

        assertTrue(created.isNew)
        assertEquals("", created.name)
        assertFalse(created.canBeSaved)
        assertTrue(created.cities.isEmpty())
        assertTrue(created.id.isNotBlank())
    }

    @Test
    fun `an existing path opens as an edit and keeps its street restrictions`() {
        val opened = AdminUiDeliveryPath(
            id = "path-a",
            name = "Parcours A",
            cities = listOf(boujailles),
            deliveryDay = "TUESDAY",
            deliveryFrequency = "BIWEEKLY_ODD"
        ).toDraft()

        assertFalse(opened.isNew)
        assertEquals("path-a", opened.id)
        assertEquals("BIWEEKLY_ODD", opened.deliveryFrequency)
        assertEquals(listOf("Rue du Moulin"), opened.cities.single().streets)
    }

    @Test
    fun `a draft round-trips through the model the repository writes`() {
        val roundTripped = draft.toAdminUiDeliveryPath().toDraft()

        assertEquals(draft.copy(isNew = false), roundTripped)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Validation
    ///////////////////////////////////////////////////////////////////////////

    @Test
    fun `a complete draft can be saved`() {
        assertTrue(draft.canBeSaved)
    }

    @Test
    fun `a draft without a name, a city or a day cannot be saved`() {
        assertFalse(draft.copy(name = "  ").canBeSaved)
        assertFalse(draft.copy(cities = emptyList()).canBeSaved)
        assertFalse(draft.copy(deliveryDay = "").canBeSaved)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Cities
    ///////////////////////////////////////////////////////////////////////////

    @Test
    fun `a new city is appended at the end`() {
        val updated = draft.withCityAdded(DeliveryCity("Bulle", 25560))

        assertEquals(listOf("Levier", "Boujailles", "Frasne", "Bulle"), updated.cities.map { it.name })
    }

    /** Two identical stops would send the van to the same place twice. */
    @Test
    fun `adding a city already on the path changes nothing`() {
        val updated = draft.withCityAdded(DeliveryCity("levier", 25270))

        assertEquals(draft, updated)
    }

    @Test
    fun `the same city name in another postcode is a different stop`() {
        val updated = draft.withCityAdded(DeliveryCity("Levier", 25000))

        assertEquals(4, updated.cities.size)
    }

    @Test
    fun `removing a city drops only that one`() {
        val updated = draft.withCityRemovedAt(1)

        assertEquals(listOf("Levier", "Frasne"), updated.cities.map { it.name })
    }

    @Test
    fun `removing out of bounds changes nothing`() {
        assertEquals(draft, draft.withCityRemovedAt(9))
        assertEquals(draft, draft.withCityRemovedAt(-1))
    }

    ///////////////////////////////////////////////////////////////////////////
    // Ordering — this is the order the van drives, and the cached coordinates
    // are aligned with it positionally.
    ///////////////////////////////////////////////////////////////////////////

    @Test
    fun `moving a city up swaps it with its predecessor`() {
        val updated = draft.withCityMoved(1, MoveDirection.UP)

        assertEquals(listOf("Boujailles", "Levier", "Frasne"), updated.cities.map { it.name })
    }

    @Test
    fun `moving a city down swaps it with its successor`() {
        val updated = draft.withCityMoved(1, MoveDirection.DOWN)

        assertEquals(listOf("Levier", "Frasne", "Boujailles"), updated.cities.map { it.name })
    }

    /** Wrapping around would silently reorder the whole tournée. */
    @Test
    fun `moving past either end changes nothing`() {
        assertEquals(draft, draft.withCityMoved(0, MoveDirection.UP))
        assertEquals(draft, draft.withCityMoved(2, MoveDirection.DOWN))
    }

    ///////////////////////////////////////////////////////////////////////////
    // Streets
    ///////////////////////////////////////////////////////////////////////////

    @Test
    fun `setting streets touches only the targeted city`() {
        val updated = draft.withStreetsAt(0, listOf("Grande Rue"))

        assertEquals(listOf("Grande Rue"), updated.cities[0].streets)
        assertEquals(listOf("Rue du Moulin"), updated.cities[1].streets)
        assertTrue(updated.cities[2].streets.isEmpty())
    }

    /** An empty street list is how whole-commune coverage is expressed. */
    @Test
    fun `clearing the streets restores whole-city coverage`() {
        val updated = draft.withStreetsAt(1, emptyList())

        assertTrue(updated.cities[1].coversWholeCity)
    }

    @Test
    fun `a street is appended trimmed`() {
        assertEquals(listOf("Grande Rue"), emptyList<String>().plusStreet("  Grande Rue  "))
    }

    @Test
    fun `a blank street is ignored`() {
        assertEquals(listOf("Grande Rue"), listOf("Grande Rue").plusStreet("   "))
    }

    /** Same normalization as the customer matcher, so the two cannot disagree. */
    @Test
    fun `a street already listed is not added again whatever the case or accents`() {
        val streets = listOf("Rue des Prés")

        assertEquals(streets, streets.plusStreet("rue des pres"))
        assertEquals(streets, streets.plusStreet("RUE DES PRÉS"))
    }

    @Test
    fun `a street that only differs by punctuation is still a duplicate`() {
        val streets = listOf("Rue du Moulin")

        assertEquals(streets, streets.plusStreet("Rue du Moulin."))
    }

    @Test
    fun `a genuinely different street is added`() {
        val streets = listOf("Rue du Moulin")

        assertEquals(
            listOf("Rue du Moulin", "Rue du Moulin Neuf"),
            streets.plusStreet("Rue du Moulin Neuf")
        )
    }
}
