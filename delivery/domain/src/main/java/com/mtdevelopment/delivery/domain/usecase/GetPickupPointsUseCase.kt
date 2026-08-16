package com.mtdevelopment.delivery.domain.usecase

import com.mtdevelopment.core.model.PickupPoint
import com.mtdevelopment.delivery.domain.repository.FirestorePathRepository

/**
 * Fetches the places an order can be collected at, for the customer journey.
 *
 * Failure and "nothing configured" are reported the same way on purpose — as a failure. The
 * shop always has at least its own counter, so an empty read means the read did not happen,
 * and telling a customer "no pickup available" when the truth is "we could not check" would
 * cost a sale for no reason.
 */
class GetPickupPointsUseCase(
    private val firestorePathRepository: FirestorePathRepository
) {
    operator fun invoke(
        onSuccess: (List<PickupPoint>) -> Unit,
        onFailure: () -> Unit
    ) {
        firestorePathRepository.getAllPickupPoints(onSuccess = onSuccess, onFailure = onFailure)
    }
}
