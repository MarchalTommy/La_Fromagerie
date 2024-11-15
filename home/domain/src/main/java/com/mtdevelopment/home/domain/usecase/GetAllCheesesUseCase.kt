package com.mtdevelopment.home.domain.usecase

import com.mtdevelopment.home.domain.model.Product
import com.mtdevelopment.home.domain.repository.RoomRepository
import kotlinx.coroutines.flow.last

class GetAllCheesesUseCase(
    private val roomRepository: RoomRepository
) {
    suspend operator fun invoke(onSuccess: (List<Product>) -> Unit) {
        roomRepository.getCheeses {
            onSuccess.invoke(it)
        }
    }
}