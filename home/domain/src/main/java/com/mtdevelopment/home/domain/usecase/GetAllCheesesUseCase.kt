package com.mtdevelopment.home.domain.usecase

import com.mtdevelopment.core.model.Product
import com.mtdevelopment.home.domain.repository.RoomRepository

class GetAllCheesesUseCase(
    private val roomRepository: RoomRepository
) {
    suspend operator fun invoke(onSuccess: (List<Product>) -> Unit) {
        roomRepository.getCheeses {
            onSuccess.invoke(it)
        }
    }
}