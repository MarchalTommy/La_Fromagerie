package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.DeliveryPath

class AddNewPathUseCase(
    private val firebaseRepository: FirebaseAdminRepository
) {
    operator fun invoke(path: DeliveryPath) {
        firebaseRepository.addNewDeliveryPath(path)
    }
}