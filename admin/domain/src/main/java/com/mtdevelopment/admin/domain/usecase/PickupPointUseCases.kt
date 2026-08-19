package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.domain.toTimeStamp
import com.mtdevelopment.core.model.PickupPoint
import com.mtdevelopment.core.model.PickupPointType

/**
 * Retrieves every pickup point, ordered the way the shop thinks about them: the shop first,
 * then the market dates chronologically. Firestore returns documents in no useful order, so
 * sorting here keeps every consumer consistent without each re-deciding.
 */
class GetAllPickupPointsUseCase(
    private val firebaseAdminRepository: FirebaseAdminRepository
) {
    suspend operator fun invoke(): Result<List<PickupPoint>> {
        return firebaseAdminRepository.getAllPickupPoints().map { points ->
            points.sortedWith(
                compareBy<PickupPoint> { it.type != PickupPointType.SHOP }
                    .thenBy { it.date?.toTimeStamp() ?: 0L }
                    .thenBy { it.label }
            )
        }
    }
}

/**
 * Adds a new pickup point.
 *
 * Refuses an incomplete one rather than storing it: a shop with no opening day or a market
 * with no date is invisible to every customer, so saving it would look like a silent failure.
 */
class AddNewPickupPointUseCase(
    private val firebaseAdminRepository: FirebaseAdminRepository
) {
    suspend operator fun invoke(point: PickupPoint): Result<Unit> {
        if (!point.canBeSaved) {
            return Result.failure(IllegalArgumentException("Point de retrait incomplet"))
        }
        return firebaseAdminRepository.addNewPickupPoint(point)
    }
}

/** Updates an existing pickup point, with the same completeness guard as creation. */
class UpdatePickupPointUseCase(
    private val firebaseAdminRepository: FirebaseAdminRepository
) {
    suspend operator fun invoke(point: PickupPoint): Result<Unit> {
        if (!point.canBeSaved) {
            return Result.failure(IllegalArgumentException("Point de retrait incomplet"))
        }
        return firebaseAdminRepository.updatePickupPoint(point)
    }
}

/** Deletes a pickup point. */
class DeletePickupPointUseCase(
    private val firebaseAdminRepository: FirebaseAdminRepository
) {
    suspend operator fun invoke(point: PickupPoint): Result<Unit> {
        return firebaseAdminRepository.deletePickupPoint(point)
    }
}
