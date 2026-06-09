package com.mtdevelopment.delivery.domain.usecase

import com.mtdevelopment.core.repository.SharedDatastore
import com.mtdevelopment.delivery.domain.model.DeliveryPath
import com.mtdevelopment.delivery.domain.repository.FirestorePathRepository
import com.mtdevelopment.delivery.domain.repository.RoomDeliveryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Use case to retrieve all delivery paths, implementing a synchronization strategy between 
 * the remote Firestore database and the local Room database.
 * 
 * Logic flow:
 * 1. Checks if a refresh is needed by comparing local state with the `shouldRefreshPaths` flag from [SharedDatastore].
 * 2. If refresh is needed (or forced):
 *    - Fetches paths from [FirestorePathRepository].
 *    - Persists each path into the local [RoomDeliveryRepository].
 *    - Performs a cleanup: deletes paths from local Room that are no longer present in Firestore.
 *    - Resets the `shouldRefreshPaths` flag to false.
 * 3. If no refresh is needed:
 *    - Fetches paths directly from the local [RoomDeliveryRepository] for faster access and offline support.
 */
class GetAllDeliveryPathsUseCase(
    private val roomRepository: RoomDeliveryRepository,
    private val sharedDatastore: SharedDatastore,
    private val repository: FirestorePathRepository
) {
    /**
     * Executes the use case.
     * @param forceRefresh If true, ignores the local flag and fetches from network.
     * @param withGeoJson If true, requests the full geographic data for the paths.
     * @param scope CoroutineScope used for background database operations.
     * @param onSuccess Callback for successful retrieval of paths.
     * @param onFailure Callback for errors during network fetch.
     */
    suspend operator fun invoke(
        forceRefresh: Boolean = false,
        withGeoJson: Boolean = false,
        scope: CoroutineScope,
        onSuccess: (List<DeliveryPath?>) -> Unit,
        onFailure: () -> Unit
    ) {

        val shouldRefresh = forceRefresh || sharedDatastore.shouldRefreshPaths.first()

        if (shouldRefresh) {
            repository.getAllDeliveryPaths(
                withGeoJson = withGeoJson,
                onSuccess = { pathsList ->
                    /**
                     * Cache synchronization: Persist all new/updated paths to Room
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
                         * Mark local cache as synchronized
                         */
                        sharedDatastore.setShouldRefreshPaths(false)

                        /**
                         * Cache cleanup: Remove local paths that no longer exist on the server
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
                    // In case of network failure, keep the refresh flag true for next attempt
                    scope.launch {
                        sharedDatastore.setShouldRefreshPaths(true)
                        onFailure.invoke()
                    }
                }
            )
        } else {
            // Fast path: Load from local database
            scope.launch {
                roomRepository.getPaths { localPathsList ->
                    onSuccess.invoke(localPathsList)
                }
            }
        }

    }
}