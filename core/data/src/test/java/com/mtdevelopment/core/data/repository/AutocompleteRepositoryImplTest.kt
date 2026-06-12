package com.mtdevelopment.core.data.repository

import com.mtdevelopment.core.model.autocomplete.AutoCompleteSuggestions
import com.mtdevelopment.core.repository.AutocompleteRepositoryImpl
import com.mtdevelopment.core.source.AutoCompleteApiDataSource
import com.mtdevelopment.core.util.NetWorkResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AutocompleteRepositoryImplTest {

    private lateinit var dataSource: AutoCompleteApiDataSource
    private lateinit var repository: AutocompleteRepositoryImpl

    @Before
    fun setUp() {
        dataSource = mockk()
        repository = AutocompleteRepositoryImpl(dataSource)
    }

    @Test
    fun `maps successful results to domain suggestions`() = runTest {
        coEvery { dataSource.getAutoCompleteSuggestions("pont") } returns NetWorkResult.Success(
            AutoCompleteSuggestions(
                results = listOf(
                    AutoCompleteSuggestions.Result(
                        city = "Pontarlier",
                        zipcode = "25300",
                        fulltext = "Pontarlier, 25300",
                        x = 6.35,
                        y = 46.90
                    )
                ),
                status = "OK"
            )
        )

        val result = repository.getAutoCompleteSuggestions("pont")

        assertEquals(1, result.size)
        val suggestion = result.first()
        assertEquals("Pontarlier", suggestion?.city)
        assertEquals("25300", suggestion?.postCode)
        assertEquals("Pontarlier, 25300", suggestion?.fulltext)
        assertEquals(46.90, suggestion?.lat)
        assertEquals(6.35, suggestion?.long)
    }

    @Test
    fun `returns empty list on network error`() = runTest {
        coEvery { dataSource.getAutoCompleteSuggestions(any()) } returns NetWorkResult.Error(
            message = "timeout",
            code = "IOException"
        )

        assertTrue(repository.getAutoCompleteSuggestions("pont").isEmpty())
    }

    @Test
    fun `returns empty list when api returns no results`() = runTest {
        coEvery { dataSource.getAutoCompleteSuggestions(any()) } returns NetWorkResult.Success(
            AutoCompleteSuggestions(results = emptyList(), status = "OK")
        )

        assertTrue(repository.getAutoCompleteSuggestions("pont").isEmpty())
    }

    @Test
    fun `returns empty list when api returns null results`() = runTest {
        coEvery { dataSource.getAutoCompleteSuggestions(any()) } returns NetWorkResult.Success(
            AutoCompleteSuggestions(results = null, status = "OK")
        )

        assertTrue(repository.getAutoCompleteSuggestions("pont").isEmpty())
    }
}
