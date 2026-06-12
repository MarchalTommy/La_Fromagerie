package com.mtdevelopment.core.domain

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LogicUtilsTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    // region Price conversions

    @Test
    fun `toCentsLong rounds to nearest cent instead of truncating`() {
        // 19.99 * 100 == 1998.9999999999998 in binary floating point
        assertEquals(1999L, 19.99.toCentsLong())
        assertEquals(1050L, 10.50.toCentsLong())
        assertEquals(0L, 0.0.toCentsLong())
        assertEquals(1L, 0.01.toCentsLong())
        assertEquals(123456L, 1234.56.toCentsLong())
    }

    @Test
    fun `toPriceDouble converts cents back to double`() {
        assertEquals(19.99, 1999L.toPriceDouble(), 0.0001)
        assertEquals(0.0, 0L.toPriceDouble(), 0.0001)
    }

    @Test
    fun `toLongPrice parses plain comma separated prices`() {
        assertEquals(1999L, "19,99".toLongPrice())
        assertEquals(1999L, "19.99".toLongPrice())
        assertEquals(1050L, "10,50 €".toLongPrice())
        assertEquals(1000L, "10".toLongPrice())
    }

    @Test
    fun `toLongPrice does not lose a cent to floating point truncation`() {
        assertEquals(1998L, "19,98".toLongPrice())
        assertEquals(1999L, "19,99".toLongPrice())
        assertEquals(2001L, "20,01".toLongPrice())
    }

    @Test
    fun `toStringPrice then toLongPrice round trips`() {
        // toStringPrice uses the French locale which inserts non-breaking spaces.
        listOf(0L, 1L, 99L, 100L, 1050L, 1999L, 123456L, 100000000L).forEach { cents ->
            assertEquals(cents, cents.toStringPrice().toLongPrice())
        }
    }

    // endregion

    // region Dates

    @Test
    fun `toTimeStamp and toStringDate round trip`() {
        val date = "25/12/2025"
        assertEquals(date, date.toTimeStamp().toStringDate())
    }

    @Test
    fun `toLocalDate parses valid date`() {
        val parsed = "01/02/2024".toLocalDate()
        assertEquals(2024, parsed?.year)
        assertEquals(2, parsed?.monthValue)
        assertEquals(1, parsed?.dayOfMonth)
    }

    @Test
    fun `toLocalDate returns null on invalid input`() {
        assertNull("not a date".toLocalDate())
        assertNull("".toLocalDate())
        assertNull("2024-02-01".toLocalDate())
    }

    @Test
    fun `toTimeStamp returns zero on invalid input`() {
        assertEquals(0L, "garbage".toTimeStamp())
    }

    // endregion

    // region City normalization

    @Test
    fun `normalizeCityName removes accents dashes and extra spaces`() {
        assertEquals("saint etienne", "Saint-Étienne".normalizeCityName())
        assertEquals("besancon", "Besançon".normalizeCityName())
        assertEquals("la chaux de fonds", "La  Chaux-de-Fonds ".normalizeCityName())
    }

    @Test
    fun `isSameCity matches normalized names`() {
        assertTrue(isSameCity("Saint-Étienne", "saint etienne"))
        assertTrue(isSameCity("BESANÇON", "besancon"))
        assertFalse(isSameCity("Lyon", "Paris"))
    }

    @Test
    fun `isSameCity is false when either side is null`() {
        assertFalse(isSameCity(null, "Lyon"))
        assertFalse(isSameCity("Lyon", null))
        assertFalse(isSameCity(null, null))
    }

    // endregion

    // region List utilities

    @Test
    fun `move relocates element within bounds`() {
        val list = mutableListOf("a", "b", "c")
        list.move(0, 2)
        assertEquals(listOf("b", "c", "a"), list)
    }

    @Test
    fun `move ignores invalid indices`() {
        val list = mutableListOf("a", "b", "c")
        list.move(-1, 2)
        list.move(0, 3)
        list.move(1, 1)
        assertEquals(listOf("a", "b", "c"), list)
    }

    @Test
    fun `reorderList applies a valid permutation`() {
        val result = reorderList(listOf("a", "b", "c"), listOf(2, 0, 1))
        assertEquals(listOf("b", "c", "a"), result)
    }

    @Test
    fun `reorderList returns identity for identity permutation`() {
        val result = reorderList(listOf("a", "b"), listOf(0, 1))
        assertEquals(listOf("a", "b"), result)
    }

    @Test
    fun `reorderList returns original order when indices size mismatches`() {
        val original = listOf("a", "b", "c")
        assertEquals(original, reorderList(original, listOf(0, 1)))
        assertEquals(original, reorderList(original, emptyList()))
    }

    @Test
    fun `reorderList returns original order on null or out of bounds indices`() {
        val original = listOf("a", "b", "c")
        assertEquals(original, reorderList(original, listOf(0, null, 2)))
        assertEquals(original, reorderList(original, listOf(0, 1, 3)))
        assertEquals(original, reorderList(original, listOf(0, 1, -1)))
    }

    @Test
    fun `reorderList returns original order on duplicate indices`() {
        val original = listOf("a", "b", "c")
        assertEquals(original, reorderList(original, listOf(0, 0, 1)))
    }

    @Test
    fun `reorderList handles empty list`() {
        assertEquals(emptyList<String>(), reorderList(emptyList<String>(), emptyList()))
    }

    // endregion
}
