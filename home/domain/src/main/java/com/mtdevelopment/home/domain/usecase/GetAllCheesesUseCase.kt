package com.mtdevelopment.home.domain.usecase

import com.mtdevelopment.core.model.Product
import com.mtdevelopment.home.domain.repository.RoomHomeRepository

class GetAllCheesesUseCase(
    private val roomHomeRepository: RoomHomeRepository
) {
    suspend operator fun invoke(onSuccess: (List<Product>) -> Unit) {
        roomHomeRepository.getCheeses {
            onSuccess.invoke(it)
        }
    }
}