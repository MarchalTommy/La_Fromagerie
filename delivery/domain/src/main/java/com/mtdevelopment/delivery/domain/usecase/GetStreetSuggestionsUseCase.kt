package com.mtdevelopment.delivery.domain.usecase

import com.mtdevelopment.delivery.domain.repository.AddressApiRepository

/**
 * Suggests street names of one commune while the shop restricts a path to part of it.
 *
 * Typing street names by hand is where a split city quietly breaks: the customer matcher compares
 * the stored label against the address, so a typo or an abbreviation ("Gde Rue") silently sends
 * everyone on that street to the wrong tournée. Picking from the address database keeps the two
 * sides spelling the same thing.
 *
 * Free text remains accepted — the suggestion list is an aid, not a gate. A brand-new street the
 * database does not know yet must still be enterable.
 */
class GetStreetSuggestionsUseCase(
    private val addressApiRepository: AddressApiRepository
) {
    suspend operator fun invoke(query: String, city: String, postcode: Int): List<String> =
        addressApiRepository.getStreetSuggestions(query, city, postcode)
}
