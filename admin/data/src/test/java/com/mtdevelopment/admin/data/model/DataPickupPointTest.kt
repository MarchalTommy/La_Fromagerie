package com.mtdevelopment.admin.data.model

import com.mtdevelopment.core.model.PickupPoint
import com.mtdevelopment.core.model.PickupPointType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataPickupPointTest {

    private fun shop() = PickupPoint(
        id = "shop",
        type = PickupPointType.SHOP,
        label = "La Fromagerie",
        address = "3 Grande Rue, 25300 Pontarlier",
        latitude = 46.9036,
        longitude = 6.3553,
        timeRange = "9h-12h / 15h-19h",
        openingDays = listOf("TUESDAY", "SATURDAY"),
        closedDates = listOf("15/08/2026")
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
    fun `a shop round-trips through the Firestore DTO`() {
        assertEquals(shop(), shop().toDataPickupPoint().toPickupPoint())
    }

    @Test
    fun `a market round-trips through the Firestore DTO`() {
        assertEquals(market(), market().toDataPickupPoint().toPickupPoint())
    }

    @Test
    fun `a market never carries opening days or closures`() {
        // A draft edited as a shop and then switched to a market keeps both halves in
        // memory; only the ones that still apply reach Firestore.
        val switched = market().copy(
            openingDays = listOf("TUESDAY"),
            closedDates = listOf("01/01/2027")
        )

        val stored = switched.toDataPickupPoint()

        assertTrue(stored.opening_days.isEmpty())
        assertTrue(stored.closed_dates.isEmpty())
        assertEquals("15/08/2026", stored.date)
    }

    @Test
    fun `a shop never carries a market date`() {
        val switched = shop().copy(date = "15/08/2026")

        val stored = switched.toDataPickupPoint()

        assertNull(stored.date)
        assertEquals(listOf("TUESDAY", "SATURDAY"), stored.opening_days)
    }

    @Test
    fun `the type is stored as its name and read back defensively`() {
        assertEquals("MARKET", market().toDataPickupPoint().type)
        // A type written by a future app version degrades instead of failing the mapping.
        assertEquals(
            PickupPointType.SHOP,
            DataPickupPoint(type = "DRIVE_THROUGH").toPickupPoint().type
        )
    }

    @Test
    fun `an empty document maps to an empty shop rather than failing`() {
        // Firestore's reflective mapper needs every field defaulted; this is the shape a
        // document written by a future version with fewer keys arrives as.
        val mapped = DataPickupPoint().toPickupPoint()

        assertEquals(PickupPointType.SHOP, mapped.type)
        assertEquals("", mapped.label)
        assertNull(mapped.date)
        assertNull(mapped.latitude)
        assertTrue(mapped.openingDays.isEmpty())
    }
}
