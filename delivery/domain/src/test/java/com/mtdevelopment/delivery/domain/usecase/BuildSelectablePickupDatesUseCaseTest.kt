package com.mtdevelopment.delivery.domain.usecase

import com.mtdevelopment.core.model.PickupPoint
import com.mtdevelopment.core.model.PickupPointType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

class BuildSelectablePickupDatesUseCaseTest {

    private val useCase = BuildSelectablePickupDatesUseCase()

    // Wednesday.
    private val now: LocalDateTime = LocalDateTime.of(2026, 8, 12, 9, 0)

    private fun shop(
        openingDays: List<String> = listOf("TUESDAY", "SATURDAY"),
        closedDates: List<String> = emptyList()
    ) = PickupPoint(
        id = "shop",
        type = PickupPointType.SHOP,
        label = "La Fromagerie",
        address = "3 Grande Rue, 25300 Pontarlier",
        timeRange = "9h-12h",
        openingDays = openingDays,
        closedDates = closedDates
    )

    private fun market(id: String, date: String, label: String = "Marché") = PickupPoint(
        id = id,
        type = PickupPointType.MARKET,
        label = label,
        address = "Place Saint-Bénigne, 25300 Pontarlier",
        timeRange = "8h-13h",
        date = date
    )

    @Test
    fun `the shop yields its next opening days in order`() {
        val dates = useCase.invoke(listOf(shop()), now, limit = 4)

        assertEquals(
            listOf(
                LocalDate.of(2026, 8, 15), // Saturday
                LocalDate.of(2026, 8, 18), // Tuesday
                LocalDate.of(2026, 8, 22),
                LocalDate.of(2026, 8, 25)
            ),
            dates.map { it.date }
        )
    }

    @Test
    fun `a closed date is skipped even though it falls on an opening day`() {
        // The only way a recurring shop can be shut: without this, "open every Saturday"
        // admits no holiday at all.
        val dates = useCase.invoke(listOf(shop(closedDates = listOf("15/08/2026"))), now, limit = 2)

        assertEquals(
            listOf(LocalDate.of(2026, 8, 18), LocalDate.of(2026, 8, 22)),
            dates.map { it.date }
        )
    }

    @Test
    fun `a shop with no opening day yields nothing`() {
        assertTrue(useCase.invoke(listOf(shop(openingDays = emptyList())), now).isEmpty())
    }

    @Test
    fun `an unparseable opening day is ignored, the others still work`() {
        val dates = useCase.invoke(
            listOf(shop(openingDays = listOf("SATURDAY", "CHEESEDAY"))),
            now,
            limit = 2
        )

        assertEquals(
            listOf(LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 22)),
            dates.map { it.date }
        )
    }

    @Test
    fun `a market yields its single date, and a past one yields nothing`() {
        val upcoming = useCase.invoke(listOf(market("m1", "20/08/2026")), now)
        val past = useCase.invoke(listOf(market("m1", "01/08/2026")), now)

        assertEquals(listOf(LocalDate.of(2026, 8, 20)), upcoming.map { it.date })
        assertTrue(past.isEmpty())
    }

    @Test
    fun `two markets on the same day are both offered`() {
        // Unlike delivery dates, these are not deduplicated: they are two different places
        // to go, and collapsing them would hide one.
        val dates = useCase.invoke(
            listOf(
                market("m1", "15/08/2026", "Marché de Pontarlier"),
                market("m2", "15/08/2026", "Marché de Frasne")
            ),
            now
        )

        assertEquals(2, dates.size)
        assertEquals(setOf("m1", "m2"), dates.map { it.pointId }.toSet())
    }

    @Test
    fun `the cut-off is the day before at noon, exactly as for delivery`() {
        // Friday 11:59 — the Saturday market is still open for orders.
        val justBefore = useCase.invoke(
            listOf(market("m1", "15/08/2026")),
            LocalDateTime.of(2026, 8, 14, 11, 59)
        )
        // Friday 12:01 — closed.
        val justAfter = useCase.invoke(
            listOf(market("m1", "15/08/2026")),
            LocalDateTime.of(2026, 8, 14, 12, 1)
        )

        assertFalse(justBefore.single().isPastDeadline)
        assertTrue(justAfter.single().isPastDeadline)
    }

    @Test
    fun `a date past the cut-off is still listed, not removed`() {
        val dates = useCase.invoke(
            listOf(market("m1", "15/08/2026")),
            LocalDateTime.of(2026, 8, 14, 18, 0)
        )

        assertEquals(1, dates.size)
        assertTrue(dates.single().isPastDeadline)
    }

    @Test
    fun `each date carries the point it belongs to, for the order snapshot`() {
        val dates = useCase.invoke(listOf(market("m1", "20/08/2026", "Marché de Frasne")), now)

        val date = dates.single()
        assertEquals("m1", date.pointId)
        assertEquals("Marché de Frasne", date.pointLabel)
        assertEquals("Place Saint-Bénigne, 25300 Pontarlier", date.pointAddress)
        assertEquals("8h-13h", date.timeRange)
    }

    @Test
    fun `no points yields no dates`() {
        assertTrue(useCase.invoke(emptyList(), now).isEmpty())
    }

    /**
     * The shop writes closures with an ASCII dd/MM/yyyy formatter, so the comparison side has
     * to produce ASCII too. Reformatting on the device locale turned every closure into a
     * string no stored date could match, and the shop appeared open on days it had declared
     * shut — silently, and only for some customers.
     */
    @Test
    fun `a closure is respected on a locale that does not write latin digits`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG-u-nu-arab"))

            val dates = useCase.invoke(
                listOf(shop(closedDates = listOf("15/08/2026"))),
                now,
                limit = 2
            )

            assertEquals(
                listOf(LocalDate.of(2026, 8, 18), LocalDate.of(2026, 8, 22)),
                dates.map { it.date }
            )
        } finally {
            Locale.setDefault(previous)
        }
    }
}
