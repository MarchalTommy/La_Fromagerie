package com.mtdevelopment.delivery.domain.usecase

import com.mtdevelopment.delivery.domain.repository.AddressApiRepository

class GetAutocompleteSuggestionsUseCase(
    private val addressApiRepository: AddressApiRepository
) {
    suspend operator fun invoke(query: String) =
        addressApiRepository.getAutoCompleteSuggestions(query)
}