package com.mtdevelopment.delivery.presentation.model

import com.mtdevelopment.core.domain.isSameCity
import com.mtdevelopment.core.model.AutoCompleteSuggestion

/**
 * Keeps only the suggestions that name a commune, dropping the hamlets and street addresses inside
 * one.
 *
 * The autocomplete endpoint is asked for `type=StreetAddress`, so it answers with places — and a
 * place carries the commune that contains it. Typing `Malpa` really returns, in this order:
 *
 * ```
 * city='Villers-le-Lac'  fulltext='Malpas, 25130 Villers-le-Lac'   <- the hamlet Malpas
 * city='Malpas'          fulltext='Malpas, 25160 Malpas'           <- the commune Malpas
 * city='Besançon'        fulltext='chemin de malpas, 25000 Besançon'
 * ```
 *
 * The dropdown shows `fulltext`, so the first two lines both read "Malpas" while adding the first
 * one puts **Villers-le-Lac, 30 km away** on the tournée. The shop has no way to see the difference.
 *
 * A path stops at communes, never at hamlets, so the filter keeps a suggestion only when the place
 * it names *is* its own commune — which is exactly what `fulltext` starting with `city` expresses.
 *
 * Deliberately scoped to the admin city picker rather than applied inside the shared autocomplete
 * pipeline: on the customer side a hamlet or a street IS the answer, and filtering there would
 * reject perfectly good addresses.
 */
fun List<AutoCompleteSuggestion>.communesOnly(): List<AutoCompleteSuggestion> =
    filter { suggestion ->
        val city = suggestion.city?.takeIf { it.isNotBlank() } ?: return@filter false
        if (suggestion.postCode.isNullOrBlank()) return@filter false
        val placeName = suggestion.fulltext?.substringBefore(',')?.trim()
        // No fulltext to check against: trust the commune field rather than drop a usable entry.
        placeName.isNullOrBlank() || isSameCity(placeName, city)
    }
