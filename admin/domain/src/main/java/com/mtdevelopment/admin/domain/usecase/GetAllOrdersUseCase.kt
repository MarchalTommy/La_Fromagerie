package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.Order
import kotlinx.coroutines.flow.Flow

class GetAllOrdersUseCase(
    private val firebaseAdminRepository: FirebaseAdminRepository
) {
    operator fun invoke(): Flow<List<Order>> {
        return firebaseAdminRepository.getAllOrders()
    }
}
