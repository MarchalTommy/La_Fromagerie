package com.mtdevelopment.delivery.domain.usecase

import com.mtdevelopment.core.model.DeliveryCity
import com.mtdevelopment.delivery.domain.model.DeliveryPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The reference instant for every test is Monday 3 August 2026 at 08:00, ISO week 32 (even).
 * Picking a Monday keeps the arithmetic readable: the next Tuesday is the following day and the
 * next Friday is four days later, both inside the same week.
 */
class BuildSelectableDeliveryDatesUseCaseTest {

    private val useCase = BuildSelectableDeliveryDatesUseCase()

    private val monday3August = LocalDateTime.of(2026, 8, 3, 8, 0)

    private fun path(
        id: String,
        name: String = id,
        day: String,
        frequency: String = "WEEKLY"
    ) = DeliveryPath(
        id = id,
        pathName = name,
        cities = listOf(DeliveryCity("Boujailles", 25560)),
        locations = listOf(46.85 to 6.15),
        deliveryDay = day,
        deliveryFrequency = frequency,
        geoJson = null
    )

    @Test
    fun `weekly path yields the next four occurrences of its day`() {
        val result = useCase(listOf(path("a", day = "TUESDAY")), monday3August)

        assertEquals(
            listOf(
                LocalDate.of(2026, 8, 4),
                LocalDate.of(2026, 8, 11),
                LocalDate.of(2026, 8, 18),
                LocalDate.of(2026, 8, 25)
            ),
            result.map { it.date }
        )
        assertTrue(result.all { it.pathId == "a" })
    }

    @Test
    fun `biweekly even path only yields even weeks`() {
        val result = useCase(listOf(path("a", day = "TUESDAY", frequency = "BIWEEKLY_EVEN")), monday3August)

        // Weeks 32, 34, 36, 38 — every other Tuesday starting with the current, even, week.
        assertEquals(
            listOf(
                LocalDate.of(2026, 8, 4),
                LocalDate.of(2026, 8, 18),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 15)
            ),
            result.map { it.date }
        )
    }

    @Test
    fun `biweekly odd path skips the current even week`() {
        val result = useCase(listOf(path("a", day = "TUESDAY", frequency = "BIWEEKLY_ODD")), monday3August)

        assertEquals(LocalDate.of(2026, 8, 11), result.first().date)
        assertEquals(4, result.size)
    }

    @Test
    fun `two paths are merged in chronological order and each date keeps its own path`() {
        val result = useCase(
            listOf(
                path("a", name = "Parcours A", day = "FRIDAY"),
                path("b", name = "Parcours B", day = "TUESDAY")
            ),
            monday3August
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 8, 4) to "b",  // Tuesday
                LocalDate.of(2026, 8, 7) to "a",  // Friday
                LocalDate.of(2026, 8, 11) to "b",
                LocalDate.of(2026, 8, 14) to "a"
            ),
            result.map { it.date to it.pathId }
        )
    }

    /**
     * Two tournées on the same day are indistinguishable to the customer — the order records only
     * the date — so offering the day twice would look like a bug.
     */
    @Test
    fun `paths sharing a delivery day produce a single tile per date`() {
        val result = useCase(
            listOf(path("a", day = "TUESDAY"), path("b", day = "TUESDAY")),
            monday3August
        )

        assertEquals(4, result.size)
        assertEquals(result.map { it.date }.distinct(), result.map { it.date })
        assertTrue(result.all { it.pathId == "a" })
    }

    @Test
    fun `a date is past its deadline once the day before at noon has passed`() {
        // Monday 12:01 — the Tuesday cut-off (Monday noon) has just closed.
        val justAfterCutoff = LocalDateTime.of(2026, 8, 3, 12, 1)

        val result = useCase(listOf(path("a", day = "TUESDAY")), justAfterCutoff)

        assertTrue(result.first().isPastDeadline)
        assertFalse(result[1].isPastDeadline)
    }

    @Test
    fun `a date is still open right on the deadline`() {
        val onCutoff = LocalDateTime.of(2026, 8, 3, 12, 0)

        val result = useCase(listOf(path("a", day = "TUESDAY")), onCutoff)

        assertFalse(result.first().isPastDeadline)
    }

    @Test
    fun `an unparseable delivery day falls back to friday instead of crashing`() {
        val result = useCase(listOf(path("a", day = "Mardi")), monday3August)

        assertEquals(LocalDate.of(2026, 8, 7), result.first().date)
    }

    @Test
    fun `no paths yields no dates`() {
        assertEquals(emptyList<SelectableDeliveryDate>(), useCase(emptyList(), monday3August))
    }

    @Test
    fun `the limit caps the merged list rather than each path`() {
        val result = useCase(
            listOf(path("a", day = "FRIDAY"), path("b", day = "TUESDAY")),
            monday3August,
            limit = 3
        )

        assertEquals(3, result.size)
    }
}
