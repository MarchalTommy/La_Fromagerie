package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.domain.toTimeStamp
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus

/**
 * Use case to retrieve all orders from the Firebase database.
 *
 * Orders with a failed payment ([OrderStatus.CANCELED], the terminal status written when a
 * SumUp checkout fails) are excluded here so they never reach the UI state. The remaining
 * orders are sorted by delivery date, newest first, so consumers get the same deterministic
 * order regardless of the backend's document order.
 */
class GetAllOrdersUseCase(
    private val firebaseAdminRepository: FirebaseAdminRepository
) {
    /**
     * Executes the use case.
     * @param onSuccess Callback invoked with the filtered and sorted list of orders,
     * or null if an error occurs.
     */
    suspend operator fun invoke(onSuccess: (List<Order>?) -> Unit) {
        firebaseAdminRepository.getAllOrders { orders ->
            onSuccess.invoke(
                orders
                    ?.filterNot { it.status == OrderStatus.CANCELED }
                    ?.sortedByDescending { it.deliveryDate.toTimeStamp() }
            )
        }
    }

}
