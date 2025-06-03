package com.mtdevelopment.core.domain

import android.location.Location
import android.util.Log
import java.text.NumberFormat
import java.util.Locale


fun Double.toCentsLong(): Long {
    return (this * 100).toLong()
}

fun Long.toPriceDouble(): Double {
    return this.toDouble() / 100
}

fun Long.toStringPrice(): String {
    val tempDouble = this / 100.0
    val format = NumberFormat.getCurrencyInstance(Locale.FRANCE)
    return format.format(tempDouble)
}

fun String.toLongPrice(): Long {
    return (this.replace("€", "").replace(",", ".").trim().toDouble() * 100).toLong()
}

private val DATE_FORMATTER_DDMMYYYY: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
        .withZone(java.time.ZoneId.systemDefault())

fun String.toTimeStamp(): Long {
    return try {
        val localDate = java.time.LocalDate.parse(this, DATE_FORMATTER_DDMMYYYY)
        localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (e: Exception) {
        0L // Retourne 0 ou une valeur par défaut en cas d'erreur de parsing
    }
}

fun Long.toStringDate(): String {
    val instant = java.time.Instant.ofEpochMilli(this)
    return DATE_FORMATTER_DDMMYYYY.format(instant)
}

// Fonction utilitaire pour calculer la distance (utilise l'API Location d'Android)
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0] // Distance en mètres
}

// Fonction d'extension pour déplacer un élément dans une MutableList
fun <T> MutableList<T>.move(from: Int, to: Int) {
    // Vérifie si les indices sont valides
    if (from < 0 || from >= size || to < 0 || to >= size || from == to) {
        Log.w("ListMove", "Invalid move indices: from=$from, to=$to, size=$size")
        return
    }
    // Retire l'élément de sa position d'origine
    val element = removeAt(from)
    // Ajoute l'élément à la nouvelle position
    add(to, element)
}

fun <T> reorderList(list: List<T>, indices: List<Int?>): List<T> {
    val result = MutableList<T?>(list.size) { null }
    list.forEachIndexed { originalIndex, item ->
        val newPosition = indices[originalIndex] ?: 0
        result[newPosition] = item
    }
    @Suppress("UNCHECKED_CAST")
    return result as List<T>
}
