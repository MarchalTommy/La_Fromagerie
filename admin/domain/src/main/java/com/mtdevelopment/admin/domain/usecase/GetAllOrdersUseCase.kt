package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.Order

/**
 * Use case to retrieve all orders from the Firebase database.
 */
class GetAllOrdersUseCase(
    private val firebaseAdminRepository: FirebaseAdminRepository
) {
    /**
     * Executes the use case.
     * @param onSuccess Callback invoked with the list of orders, or null if an error occurs.
     */
    suspend operator fun invoke(onSuccess: (List<Order>?) -> Unit) {
        firebaseAdminRepository.getAllOrders(onSuccess)
    }

}