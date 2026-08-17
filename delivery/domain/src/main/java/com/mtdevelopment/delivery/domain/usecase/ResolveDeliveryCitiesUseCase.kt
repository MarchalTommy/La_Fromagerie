package com.mtdevelopment.delivery.domain.usecase

import com.mtdevelopment.core.domain.isSameCity
import com.mtdevelopment.core.model.DeliveryCity
import com.mtdevelopment.delivery.domain.model.CommuneLookup
import com.mtdevelopment.delivery.domain.repository.AddressApiRepository

/**
 * What the address API says about one city the shop put on a path.
 *
 * @property submitted The city exactly as stored, spelling included.
 * @property canonical The same city under the API's spelling and carrying its center, or null when
 *   the API knows no such commune. The postcode and the street restriction are never touched — see
 *   [ResolveDeliveryCitiesUseCase].
 * @property checked False when the API could not be reached for this city, in which case
 *   [canonical] being null means nothing at all.
 */
data class CityResolution(
    val submitted: DeliveryCity,
    val canonical: DeliveryCity?,
    val checked: Boolean
) {
    /** The API answered and knows no such commune: the spelling is wrong beyond fuzzy rescue. */
    val isUnknown: Boolean get() = checked && canonical == null

    /** The commune exists but is stored under a different spelling. */
    val isMisspelled: Boolean
        get() = canonical != null && !isSameCity(submitted.name, canonical.name)

    /** Nothing to fix, or nothing knowable. */
    val isClean: Boolean get() = !isUnknown && !isMisspelled
}

/**
 * Checks the cities of a path against the address API and reports the canonical spelling of each.
 *
 * ### Why this exists
 *
 * Three cities of the real Friday tournée were stored as `Malpa`, `Oye-et-Palets` and
 * `Larbergement-sainte-marie`, against the actual communes **Malpas**, **Oye-et-Pallet** and
 * **Labergement-Sainte-Marie**. They came from the free-text city field of the path dialog that
 * PR #61 replaced, and nothing has looked at them since.
 *
 * Geocoding never complained, because the API's search is fuzzy and rescued all three — so the path
 * kept building and the map kept drawing. The customer matcher is what breaks: `isSameCity` compares
 * for strict equality after normalization, and `"malpa" != "malpas"`. A customer in Malpas is
 * therefore told their address is not on a delivery path. The failure is silent on the side that
 * would reveal it and invisible on the side that would fix it, which is exactly why it survived.
 *
 * ### What it takes from the API, and what it leaves alone
 *
 * The name — the only field the matcher compares, so the only one that has to be right — and the
 * commune's center, which the path editor stores so that reading the path back needs no geocoding.
 *
 * The postcode stays as the shop entered it, and so do the street restrictions. A commune can carry
 * several postcodes and the API returns one of them; overwriting a deliberate choice with an
 * arbitrary alternative would be a worse bug than the one being fixed. The streets are the
 * split-city configuration and are nobody else's to rewrite.
 *
 * ### Unreachable is not "wrong"
 *
 * Every resolution carries [CityResolution.checked]. Telling the shop its city list is misspelled
 * because the phone lost signal would be worse than staying quiet, so a city whose lookup did not
 * arrive is reported as unchecked and never as a problem.
 */
class ResolveDeliveryCitiesUseCase(
    private val addressApiRepository: AddressApiRepository
) {

    suspend operator fun invoke(cities: List<DeliveryCity>): List<CityResolution> =
        cities.map { city ->
            when (val lookup = addressApiRepository.lookupCommune(city.name, city.postcode)) {
                is CommuneLookup.Found -> CityResolution(
                    submitted = city,
                    // Keep postcode and streets; take the spelling and the center. A blank name
                    // from the API is not an improvement on what the shop typed.
                    canonical = city.copy(
                        name = lookup.info.name.ifBlank { city.name },
                        latitude = lookup.info.location.latitude,
                        longitude = lookup.info.location.longitude
                    ),
                    checked = true
                )

                CommuneLookup.NotFound -> CityResolution(
                    submitted = city,
                    canonical = null,
                    checked = true
                )

                CommuneLookup.Unreachable -> CityResolution(
                    submitted = city,
                    canonical = null,
                    checked = false
                )
            }
        }
}

/** The cities of this report that need the shop's attention, in path order. */
fun List<CityResolution>.problems(): List<CityResolution> = filterNot { it.isClean }

/**
 * Applies every known canonical spelling, leaving unknown and unchecked cities untouched.
 *
 * Order is preserved because it is the order the van drives, and it is what
 * [com.mtdevelopment.delivery.domain.model.DeliveryPath.locations] is aligned with.
 */
fun List<CityResolution>.withCanonicalCities(): List<DeliveryCity> =
    map { it.canonical ?: it.submitted }
