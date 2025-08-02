package com.mtdevelopment.core.repository

import com.mtdevelopment.core.model.AutoCompleteSuggestion
import com.mtdevelopment.core.model.autocomplete.AutoCompleteSuggestions
import com.mtdevelopment.core.source.AutoCompleteApiDataSource
import com.mtdevelopment.core.util.NetWorkResult

class AutocompleteRepositoryImpl(
    private val autocompleteApiDataSource: AutoCompleteApiDataSource
) : AutocompleteRepository {

    override suspend fun getAutoCompleteSuggestions(query: String): List<AutoCompleteSuggestion?> {
        val result = autocompleteApiDataSource.getAutoCompleteSuggestions(query)

        if (result is NetWorkResult.Error) {
            return emptyList()
        }

        val cleanedList = (result as? NetWorkResult<AutoCompleteSuggestions>)?.data?.results?.map {
            AutoCompleteSuggestion(
                city = it?.city,
                postCode = it?.zipcode,
                fulltext = it?.fulltext,
                lat = it?.y,
                long = it?.x
            )
        }

        return if (cleanedList?.isNotEmpty() == true) {
            cleanedList
        } else {
            emptyList()
        }
    }


}