package com.mtdevelopment.core.usecase

import com.mtdevelopment.core.repository.AutocompleteRepository

class GetAutocompleteSuggestionsUseCase(
    private val addressApiRepository: AutocompleteRepository
) {
    suspend operator fun invoke(query: String) =
        addressApiRepository.getAutoCompleteSuggestions(query)
}