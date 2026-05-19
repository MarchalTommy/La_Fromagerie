package com.mtdevelopment.core.domain

import android.location.Location
import android.util.Log
import java.text.NumberFormat
import java.util.Locale


/**
 * Converts a price in [Double] to its cent representation in [Long] to avoid precision issues.
 */
fun Double.toCentsLong(): Long {
    return (this * 100).toLong()
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
 */
fun String.toLongPrice(): Long {
    return (this.replace("€", "").replace(",", ".").trim().toDouble() * 100).toLong()
}

/**
 * Formatter for dates in "dd/MM/yyyy" format using the system timezone.
 */
private val DATE_FORMATTER_DDMMYYYY: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
        .withZone(java.time.ZoneId.systemDefault())

/**
 * Parses a "dd/MM/yyyy" date string into a Unix timestamp (milliseconds).
 */
fun String.toTimeStamp(): Long {
    return try {
        val localDate = java.time.LocalDate.parse(this, DATE_FORMATTER_DDMMYYYY)
        localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (e: Exception) {
        0L 
    }
}

/**
 * Formats a Unix timestamp (milliseconds) into a "dd/MM/yyyy" date string.
 */
fun Long.toStringDate(): String {
    val instant = java.time.Instant.ofEpochMilli(this)
    return DATE_FORMATTER_DDMMYYYY.format(instant)
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
 * @param list The original list of items.
 * @param indices A list where `indices[i]` is the new position of `list[i]`.
 * @return A new list with items in their optimized positions.
 */
fun <T> reorderList(list: List<T>, indices: List<Int?>): List<T> {
    val result = MutableList<T?>(list.size) { null }
    list.forEachIndexed { originalIndex, item ->
        val newPosition = indices[originalIndex] ?: 0
        result[newPosition] = item
    }
    @Suppress("UNCHECKED_CAST")
    return result as List<T>
}
