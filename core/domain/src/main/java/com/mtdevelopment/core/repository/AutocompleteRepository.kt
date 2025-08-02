package com.mtdevelopment.core.repository

import com.mtdevelopment.core.model.AutoCompleteSuggestion

interface AutocompleteRepository {

    suspend fun getAutoCompleteSuggestions(
        query: String
    ): List<AutoCompleteSuggestion?>
}