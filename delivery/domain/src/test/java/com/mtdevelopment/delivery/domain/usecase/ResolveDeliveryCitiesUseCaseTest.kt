package com.mtdevelopment.delivery.domain.usecase

import com.google.android.gms.maps.model.LatLng
import com.mtdevelopment.core.model.DeliveryCity
import com.mtdevelopment.delivery.domain.model.CityInformation
import com.mtdevelopment.delivery.domain.model.CommuneLookup
import com.mtdevelopment.delivery.domain.repository.AddressApiRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Encodes the three real misspellings found on the Friday tournée "Le Haut" on 2026-08-17, stored by
 * the free-text city field that PR #61 replaced. Fuzzy geocoding rescued all three, so the path kept
 * building — while `isSameCity` rejected every customer living in them.
 */
class ResolveDeliveryCitiesUseCaseTest {

    private lateinit var addressApiRepository: AddressApiRepository
    private lateinit var useCase: ResolveDeliveryCitiesUseCase

    private fun found(name: String, zip: Int) = CommuneLookup.Found(
        CityInformation(name = name, zip = zip, location = LatLng(46.80, 6.29))
    )

    @Before
    fun setUp() {
        addressApiRepository = mockk()
        useCase = ResolveDeliveryCitiesUseCase(addressApiRepository)
    }

    @Test
    fun `a correctly spelled commune is reported clean`() = runTest {
        coEvery { addressApiRepository.lookupCommune("Boujailles", 25560) } returns
                found("Boujailles", 25560)

        val result = useCase(listOf(DeliveryCity("Boujailles", 25560)))

        assertTrue(result.single().isClean)
        assertFalse(result.single().isMisspelled)
        assertTrue(result.problems().isEmpty())
    }

    @Test
    fun `Malpa is reported as Malpas`() = runTest {
        coEvery { addressApiRepository.lookupCommune("Malpa", 25160) } returns found("Malpas", 25160)

        val result = useCase(listOf(DeliveryCity("Malpa", 25160)))

        assertTrue(result.single().isMisspelled)
        assertEquals("Malpas", result.single().canonical?.name)
        assertEquals(listOf("Malpas"), result.withCanonicalNames().map { it.name })
    }

    @Test
    fun `the three real misspellings of the Friday path are all caught`() = runTest {
        coEvery { addressApiRepository.lookupCommune("Malpa", 25160) } returns found("Malpas", 25160)
        coEvery { addressApiRepository.lookupCommune("Oye-et-Palets", 25160) } returns
                found("Oye-et-Pallet", 25160)
        coEvery { addressApiRepository.lookupCommune("Larbergement-sainte-marie", 25160) } returns
                found("Labergement-Sainte-Marie", 25160)
        coEvery { addressApiRepository.lookupCommune("Frasne", 25560) } returns found("Frasne", 25560)

        val result = useCase(
            listOf(
                DeliveryCity("Malpa", 25160),
                DeliveryCity("Oye-et-Palets", 25160),
                DeliveryCity("Larbergement-sainte-marie", 25160),
                DeliveryCity("Frasne", 25560)
            )
        )

        assertEquals(3, result.problems().size)
        assertEquals(
            listOf("Malpas", "Oye-et-Pallet", "Labergement-Sainte-Marie", "Frasne"),
            result.withCanonicalNames().map { it.name }
        )
    }

    /**
     * `isSameCity` normalizes accents, dashes and case, so a difference in only those is the same
     * commune and must not be paraded as a correction.
     */
    @Test
    fun `a difference in case or accents alone is not a misspelling`() = runTest {
        coEvery { addressApiRepository.lookupCommune("METABIEF", 25370) } returns
                found("Métabief", 25370)

        val result = useCase(listOf(DeliveryCity("METABIEF", 25370)))

        assertFalse(result.single().isMisspelled)
        assertTrue(result.single().isClean)
    }

    @Test
    fun `an unknown commune is reported as unknown and has no canonical form`() = runTest {
        coEvery { addressApiRepository.lookupCommune("Nulle-Part", 99999) } returns
                CommuneLookup.NotFound

        val result = useCase(listOf(DeliveryCity("Nulle-Part", 99999)))

        assertTrue(result.single().isUnknown)
        assertEquals(null, result.single().canonical)
    }

    /**
     * The rule that stops the banner from lying: a lookup that never arrived says nothing about the
     * spelling. Accusing the shop's data because the phone lost signal would be worse than silence.
     */
    @Test
    fun `an unreachable API is never reported as a spelling problem`() = runTest {
        coEvery { addressApiRepository.lookupCommune(any(), any()) } returns
                CommuneLookup.Unreachable

        val result = useCase(listOf(DeliveryCity("Boujailles", 25560)))

        assertFalse(result.single().isUnknown)
        assertFalse(result.single().isMisspelled)
        assertTrue(result.problems().isEmpty())
    }

    @Test
    fun `an unchecked city keeps its submitted spelling when names are applied`() = runTest {
        coEvery { addressApiRepository.lookupCommune(any(), any()) } returns
                CommuneLookup.Unreachable

        val result = useCase(listOf(DeliveryCity("Malpa", 25160)))

        assertEquals(listOf("Malpa"), result.withCanonicalNames().map { it.name })
    }

    /**
     * A commune can carry several postcodes and the API returns one of them; overwriting a
     * deliberate choice with an arbitrary alternative would be a worse bug than the one being fixed.
     * Street restrictions are equally untouchable — they are the split-city configuration.
     */
    @Test
    fun `only the name is canonicalized, postcode and streets are preserved`() = runTest {
        coEvery { addressApiRepository.lookupCommune("Malpa", 25160) } returns found("Malpas", 25999)

        val result = useCase(
            listOf(DeliveryCity("Malpa", 25160, listOf("Rue de Pontarlier")))
        )

        val canonical = result.single().canonical
        assertEquals("Malpas", canonical?.name)
        assertEquals(25160, canonical?.postcode)
        assertEquals(listOf("Rue de Pontarlier"), canonical?.streets)
    }

    @Test
    fun `a blank name from the API is not an improvement on what the shop typed`() = runTest {
        coEvery { addressApiRepository.lookupCommune("Boujailles", 25560) } returns found("", 25560)

        val result = useCase(listOf(DeliveryCity("Boujailles", 25560)))

        assertEquals("Boujailles", result.single().canonical?.name)
        assertTrue(result.single().isClean)
    }

    @Test
    fun `city order is preserved because it is the order the van drives`() = runTest {
        coEvery { addressApiRepository.lookupCommune(any(), any()) } answers {
            found(firstArg<String>(), secondArg<Int>())
        }

        val cities = listOf(
            DeliveryCity("Boujailles", 25560),
            DeliveryCity("Frasne", 25560),
            DeliveryCity("Pontarlier", 25300)
        )

        assertEquals(cities, useCase(cities).withCanonicalNames())
    }

    @Test
    fun `an empty city list resolves to an empty report`() = runTest {
        assertTrue(useCase(emptyList()).isEmpty())
    }
}
