package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.DeliveryPath

class DeletePathUseCase(
    private val firebaseRepository: FirebaseAdminRepository
) {

    operator fun invoke(path: DeliveryPath) {
        firebaseRepository.deleteDeliveryPath(path)
    }

}