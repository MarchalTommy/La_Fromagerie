package com.mtdevelopment.delivery.domain.model

/**
 * Outcome of asking the address API about one commune, with the two kinds of "no" kept apart.
 *
 * [reverseGeocodeCity][com.mtdevelopment.delivery.domain.repository.AddressApiRepository.reverseGeocodeCity]
 * collapses both into `null`, which is the right shape for building a path — either way the city has
 * no usable center. It is the wrong shape for judging the data the shop typed: "the Géoplateforme
 * has never heard of this commune" is a misspelling to fix, while "the request did not arrive" says
 * nothing at all about it. Telling the shop its city list is wrong because the phone lost signal
 * would be worse than staying quiet.
 */
sealed interface CommuneLookup {

    /** The API resolved the commune. [info] carries its canonical name and its center. */
    data class Found(val info: CityInformation) : CommuneLookup

    /** The API answered, and knows no commune under that name and postcode. */
    data object NotFound : CommuneLookup

    /** The API could not be reached, or answered something unusable. Draw no conclusion. */
    data object Unreachable : CommuneLookup
}
