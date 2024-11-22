package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.Product

class AddNewProductUseCase(
    private val firebaseRepository: FirebaseAdminRepository
) {
    operator fun invoke(product: Product) {
        firebaseRepository.addNewProduct(product)
    }
}