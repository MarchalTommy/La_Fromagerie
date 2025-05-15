package com.mtdevelopment.delivery.domain.usecase

import com.mtdevelopment.core.repository.SharedDatastore
import com.mtdevelopment.delivery.domain.model.DeliveryPath
import com.mtdevelopment.delivery.domain.repository.FirestorePathRepository
import com.mtdevelopment.delivery.domain.repository.RoomDeliveryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GetAllDeliveryPathsUseCase(
    private val roomRepository: RoomDeliveryRepository,
    private val sharedDatastore: SharedDatastore,
    private val repository: FirestorePathRepository
) {
    suspend operator fun invoke(
        forceRefresh: Boolean = false,
        withGeoJson: Boolean = false,
        scope: CoroutineScope,
        onSuccess: (List<DeliveryPath?>) -> Unit,
        onFailure: () -> Unit
    ) {

        val shouldRefresh = forceRefresh || sharedDatastore.shouldRefreshPaths.first()

        /**
         * If locally known last firestore update timestamp is different from the one on the server, we need to update.
         * Else, fetch from room.
         */
        if (shouldRefresh) {
            repository.getAllDeliveryPaths(
                withGeoJson = withGeoJson,
                onSuccess = { pathsList ->
                    /**
                     * Persist all products from firebase to room
                     */
                    pathsList.forEach { path ->
                        scope.launch {
                            if (path != null) {
                                roomRepository.persistPath(path)
                            }
                        }
                    }

                    scope.launch {
                        /**
                         * Reset refresh needed flag
                         */
                        sharedDatastore.setShouldRefreshPaths(false)

                        /**
                         * Delete all products from room that are not in firebase,
                         * in case products got removed
                         */
                        roomRepository.getPaths { localPathsList ->
                            localPathsList.forEach { entity ->
                                if (!pathsList.any { data -> entity.id == data?.id }) {
                                    scope.launch {
                                        roomRepository.deletePath(entity)
                                    }
                                }
                            }
                        }
                    }

                    onSuccess(pathsList)
                }, onFailure = {
                    scope.launch {
                        sharedDatastore.setShouldRefreshPaths(true)
                        onFailure.invoke()
                    }
                }
            )
        } else {
            scope.launch {
                roomRepository.getPaths { localPathsList ->
                    onSuccess.invoke(localPathsList)
                }
            }
        }

    }
}