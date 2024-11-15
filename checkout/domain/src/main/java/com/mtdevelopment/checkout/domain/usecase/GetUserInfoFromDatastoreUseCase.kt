package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.core.repository.SharedDatastore

class GetUserInfoFromDatastoreUseCase(
    private val sharedDatastore: SharedDatastore
) {
    operator fun invoke() = sharedDatastore.userInformationFlow
}