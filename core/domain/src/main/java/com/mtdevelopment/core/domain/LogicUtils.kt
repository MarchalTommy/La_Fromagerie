package com.mtdevelopment.core.domain

import android.location.Location
import android.util.Log
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong


/**
 * Converts a price in [Double] to its cent representation in [Long] to avoid precision issues.
 * Rounds to the nearest cent so binary floating point artifacts (e.g. 19.99 * 100 = 1998.99...)
 * do not truncate a cent away.
 */
fun Double.toCentsLong(): Long {
    return (this * 100).roundToLong()
}

/**
 * Converts a price in cents [Long] to its [Double] representation.
 */
fun Long.toPriceDouble(): Double {
    return this.toDouble() / 100
}

/**
 * Formats a cent-based [Long] price into a localized currency string (e.g., "10,50 €").
 */
fun Long.toStringPrice(): String {
    val tempDouble = this / 100.0
    val format = NumberFormat.getCurrencyInstance(Locale.FRANCE)
    return format.format(tempDouble)
}

/**
 * Parses a currency string into its cent-based [Long] representation.
 * Accepts the output of [Long.toStringPrice] (which uses non-breaking spaces) and rounds to the
 * nearest cent instead of truncating.
 */
fun String.toLongPrice(): Long {
    val sanitized = this.replace("\u20AC", "")
        .replace(",", ".")
        // Strips regular and non-breaking spaces (U+00A0, U+202F) emitted by currency formatters.
        .replace("[\\s\u00A0\u202F]".toRegex(), "")
    return (sanitized.toDouble() * 100).roundToLong()
}

/**
 * Formatter for dates in "dd/MM/yyyy" format using the system timezone.
 * Pinned to [Locale.ROOT]: stored dates use ASCII digits, and a formatter built on the
 * device default locale fails to parse them on locales with non-Latin digits, silently
 * turning every timestamp-based sort into a no-op.
 */
private val DATE_FORMATTER_DDMMYYYY: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT)
        .withZone(java.time.ZoneId.systemDefault())

/**
 * Parses a "dd/MM/yyyy" date string into a LocalDate.
 */
fun String.toLocalDate(): java.time.LocalDate? {
    return try {
        java.time.LocalDate.parse(this, DATE_FORMATTER_DDMMYYYY)
    } catch (e: Exception) {
        null
    }
}

/**
 * Parses a "dd/MM/yyyy" date string into a Unix timestamp (milliseconds).
 * Uses UTC to ensure stable timestamps regardless of device timezone changes.
 */
fun String.toTimeStamp(): Long {
    return toLocalDate()?.atStartOfDay(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli() ?: 0L
}

/**
 * Formats a Unix timestamp (milliseconds) into a "dd/MM/yyyy" date string.
 * Uses UTC to ensure stable formatting.
 */
fun Long.toStringDate(): String {
    val instant = java.time.Instant.ofEpochMilli(this)
    return DATE_FORMATTER_DDMMYYYY.withZone(java.time.ZoneOffset.UTC).format(instant)
}

/**
 * Calculates the straight-line distance between two coordinates in meters.
 * Uses Android's internal [Location.distanceBetween] method.
 */
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0] 
}

/**
 * Normalizes a city name by removing accents, lowercasing, replacing dashes with spaces,
 * removing non-alphanumeric characters, and collapsing multiple spaces.
 */
fun String.normalizeCityName(): String {
    val temp = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
    val regex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    return regex.replace(temp, "")
        .lowercase()
        .replace("-", " ")
        .replace("[^a-z0-9 ]".toRegex(), "")
        .trim()
        .replace("\\s+".toRegex(), " ")
}

/**
 * Compares two city names for equality after normalization.
 */
fun isSameCity(city1: String?, city2: String?): Boolean {
    if (city1 == null || city2 == null) return false
    return city1.normalizeCityName() == city2.normalizeCityName()
}

/**
 * Helper to move an element from one index to another in a [MutableList].
 */
fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from < 0 || from >= size || to < 0 || to >= size || from == to) {
        Log.w("ListMove", "Invalid move indices: from=$from, to=$to, size=$size")
        return
    }
    val element = removeAt(from)
    add(to, element)
}

/**
 * Reorders a list based on a provided list of new indices.
 * Used for applying route optimization results from Google Maps API.
 *
 * If [indices] is not a valid permutation of the list positions (wrong size, null entries,
 * out-of-bounds values or duplicates), the original [list] order is returned unchanged instead of
 * crashing or producing a list with holes.
 *
 * @param list The original list of items.
 * @param indices A list where `indices[i]` is the new position of `list[i]`.
 * @return A new list with items in their optimized positions.
 */
fun <T> reorderList(list: List<T>, indices: List<Int?>): List<T> {
    if (list.isEmpty()) return list

    val isValidPermutation = indices.size == list.size &&
            indices.all { it != null && it in list.indices } &&
            indices.distinct().size == list.size

    if (!isValidPermutation) {
        Log.w("ListReorder", "Invalid reorder indices $indices for list of size ${list.size}")
        return list
    }

    val result = MutableList<T?>(list.size) { null }
    list.forEachIndexed { originalIndex, item ->
        result[indices[originalIndex]!!] = item
    }
    @Suppress("UNCHECKED_CAST")
    return result as List<T>
}
