package com.mtdevelopment.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PickupPointTest {

    private fun shop() = PickupPoint(
        id = "shop",
        type = PickupPointType.SHOP,
        label = "La Fromagerie",
        address = "3 Grande Rue, 25300 Pontarlier",
        timeRange = "9h-12h / 15h-19h",
        openingDays = listOf("TUESDAY", "THURSDAY", "SATURDAY")
    )

    private fun market() = PickupPoint(
        id = "market-1",
        type = PickupPointType.MARKET,
        label = "Marché de Pontarlier",
        address = "Place Saint-Bénigne, 25300 Pontarlier",
        timeRange = "8h-13h",
        date = "15/08/2026"
    )

    @Test
    fun `a complete shop and a complete market can be saved`() {
        assertTrue(shop().canBeSaved)
        assertTrue(market().canBeSaved)
    }

    @Test
    fun `a shop with no opening day cannot be saved`() {
        // It would be stored, then be invisible to every customer — which reads as a bug,
        // not as an empty configuration.
        assertFalse(shop().copy(openingDays = emptyList()).canBeSaved)
    }

    @Test
    fun `a market with no date, or an unparseable one, cannot be saved`() {
        assertFalse(market().copy(date = null).canBeSaved)
        assertFalse(market().copy(date = "").canBeSaved)
        assertFalse(market().copy(date = "2026-08-15").canBeSaved)
    }

    @Test
    fun `a point with no label or no address cannot be saved`() {
        assertFalse(shop().copy(label = "  ").canBeSaved)
        assertFalse(shop().copy(address = "").canBeSaved)
        assertFalse(market().copy(label = "").canBeSaved)
    }

    @Test
    fun `opening days are irrelevant to a market, and a date to a shop`() {
        // Switching type mid-edit leaves the other half of the draft populated; neither
        // should block saving.
        assertTrue(market().copy(openingDays = emptyList()).canBeSaved)
        assertTrue(shop().copy(date = null).canBeSaved)
    }

    @Test
    fun `an absent or unknown stored type reads as the shop`() {
        assertEquals(PickupPointType.SHOP, PickupPointType.fromStoredValue(null))
        assertEquals(PickupPointType.SHOP, PickupPointType.fromStoredValue(""))
        assertEquals(PickupPointType.SHOP, PickupPointType.fromStoredValue("DRIVE"))
        assertEquals(PickupPointType.MARKET, PickupPointType.fromStoredValue("MARKET"))
    }
}
