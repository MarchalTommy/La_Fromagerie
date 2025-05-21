package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.Order

class GetAllOrdersUseCase(
    private val firebaseAdminRepository: FirebaseAdminRepository
) {
    suspend operator fun invoke(onSuccess: (List<Order>?) -> Unit) {
        firebaseAdminRepository.getAllOrders(onSuccess)
    }

}