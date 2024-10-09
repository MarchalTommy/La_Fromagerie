package com.mtdevelopment.core.usecase

import com.mtdevelopment.core.repository.NetworkRepository
import kotlinx.coroutines.flow.Flow

class GetIsNetworkConnectedUseCase(
    private val networkRepository: NetworkRepository
) {

    operator fun invoke(): Flow<Boolean> {
        return networkRepository.isConnected
    }

}