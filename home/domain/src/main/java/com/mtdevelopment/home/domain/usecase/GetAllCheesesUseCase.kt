package com.mtdevelopment.home.domain.usecase

import com.mtdevelopment.core.model.Product
import com.mtdevelopment.home.domain.repository.RoomHomeRepository

/**
 * Use case to retrieve all cheese products from the local cache.
 */
class GetAllCheesesUseCase(
    private val roomHomeRepository: RoomHomeRepository
) {
    /**
     * Executes the use case.
     * @param onSuccess Callback invoked with the list of cheese [Product] objects.
     */
    suspend operator fun invoke(onSuccess: (List<Product>) -> Unit) {
        onSuccess(roomHomeRepository.getCheeses())
    }
}