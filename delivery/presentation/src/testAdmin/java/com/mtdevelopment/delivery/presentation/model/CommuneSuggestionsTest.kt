package com.mtdevelopment.delivery.presentation.model

import com.mtdevelopment.core.model.AutoCompleteSuggestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A delivery path stops at communes, never at hamlets. The fixtures below are the real answers the
 * Géoplateforme gave on 2026-08-17 — including the trap that makes this filter necessary.
 */
class CommuneSuggestionsTest {

    private fun suggestion(city: String, postCode: String?, fulltext: String) =
        AutoCompleteSuggestion(city = city, postCode = postCode, fulltext = fulltext)

    /**
     * Typing "Malpa" really returns the hamlet Malpas inside Villers-le-Lac **before** the commune
     * Malpas. Both lines read "Malpas" in the dropdown, which shows `fulltext`, so picking the first
     * silently puts a town 30 km off the route onto the tournée.
     */
    @Test
    fun `a hamlet bearing the searched name is dropped in favour of the commune`() {
        val result = listOf(
            suggestion("Villers-le-Lac", "25130", "Malpas, 25130 Villers-le-Lac"),
            suggestion("Malpas", "25160", "Malpas, 25160 Malpas"),
            suggestion("Besançon", "25000", "chemin de malpas, 25000 Besançon")
        ).communesOnly()

        assertEquals(listOf("Malpas"), result.map { it.city })
    }

    @Test
    fun `a hamlet with a different name inside the right commune is dropped too`() {
        val result = listOf(
            suggestion("Oye-et-Pallet", "25160", "Oye-et-Pallet, 25160 Oye-et-Pallet"),
            suggestion("Oye-et-Pallet", "25160", "Chaon, 25160 Oye-et-Pallet"),
            suggestion("Oye-et-Pallet", "25160", "Friard, 25160 Oye-et-Pallet")
        ).communesOnly()

        assertEquals(1, result.size)
        assertEquals("Oye-et-Pallet", result.single().city)
    }

    /** Accents and dashes differ freely between the two fields; `isSameCity` is what compares them. */
    @Test
    fun `accents and dashes do not make a commune look like a hamlet`() {
        val result = listOf(
            suggestion("Métabief", "25370", "METABIEF, 25370 Métabief"),
            suggestion("Les Hôpitaux-Neufs", "25370", "Les Hopitaux Neufs, 25370 Les Hôpitaux-Neufs")
        ).communesOnly()

        assertEquals(2, result.size)
    }

    @Test
    fun `a suggestion with no postcode cannot be added to a path`() {
        val result = listOf(
            suggestion("Malpas", null, "Malpas, Malpas"),
            suggestion("Malpas", "", "Malpas, Malpas")
        ).communesOnly()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `a suggestion with no city is dropped`() {
        val result = listOf(suggestion("", "25160", "Malpas, 25160 Malpas")).communesOnly()

        assertTrue(result.isEmpty())
    }

    /** No fulltext to compare against: trust the commune field rather than drop a usable entry. */
    @Test
    fun `a suggestion without fulltext is kept`() {
        val result = listOf(
            AutoCompleteSuggestion(city = "Malpas", postCode = "25160", fulltext = null)
        ).communesOnly()

        assertEquals(listOf("Malpas"), result.map { it.city })
    }

    @Test
    fun `an empty list stays empty`() {
        assertTrue(emptyList<AutoCompleteSuggestion>().communesOnly().isEmpty())
    }
}
