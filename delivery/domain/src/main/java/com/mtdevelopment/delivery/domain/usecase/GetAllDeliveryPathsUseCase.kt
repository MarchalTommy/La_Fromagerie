package com.mtdevelopment.delivery.domain.usecase

import com.mtdevelopment.core.repository.SharedDatastore
import com.mtdevelopment.delivery.domain.model.DeliveryPath
import com.mtdevelopment.delivery.domain.repository.FirestorePathRepository
import com.mtdevelopment.delivery.domain.repository.PathFetchOutcome
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
 *    - **If the fetch comes back empty, it is treated as a failure**: the flag stays set and
 *      nothing is written. An empty list must never be persisted as the truth — doing so used
 *      to wipe the cache and leave the customer with no deliverable address.
 *    - Persists each path into the local [RoomDeliveryRepository], awaiting the writes.
 *    - Deletes local paths that Firestore **no longer lists** — see [PathFetchOutcome.remoteIds].
 *    - Clears the `shouldRefreshPaths` flag only if the fetch was complete.
 *    - Emits the enriched paths **plus** any still-listed path served from cache.
 * 3. If no refresh is needed:
 *    - Fetches paths directly from the local [RoomDeliveryRepository] for faster access and offline support.
 *
 * ### Why the cleanup is the delicate part
 *
 * The cleanup used to delete every cached path absent from the enriched result, which conflated
 * "the shop deleted this tournée" with "one of its cities failed to geocode just now". The biggest
 * path is the most exposed — enrichment is all-or-nothing across its cities, so a 20-city tournée
 * fails whenever any one of 20 parallel calls does — and it was being deleted from the cache while
 * the flag was simultaneously cleared, so nothing re-fetched it. The path stayed invisible until the
 * remote `path_timestamp` moved. That is the "the Friday card disappears sometimes" bug, and it is
 * why deletion is now driven strictly by [PathFetchOutcome.remoteIds].
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
                onSuccess = { outcome ->
                    /**
                     * One coroutine for the whole sequence, because the steps are ordered:
                     * the writes must land before the cleanup reads the table back, and the
                     * refresh flag must not be cleared before either has happened. These used
                     * to be N+1 independent `launch`es, so leaving the screen mid-flight could
                     * cancel a write while the flag had already been cleared — a path silently
                     * absent from a cache marked up to date.
                     */
                    scope.launch {
                        if (outcome.paths.isEmpty()) {
                            /**
                             * Defence in depth against the "poisoned cache" failure: an empty
                             * fetch must never mark the cache as synchronized, and must never
                             * reach the cleanup below — which would delete every locally
                             * cached path because none of them appear in the empty list. The
                             * refresh flag stays set so the next attempt retries.
                             */
                            sharedDatastore.setShouldRefreshPaths(true)
                            onFailure.invoke()
                            return@launch
                        }

                        /**
                         * Cache synchronization: persist all new/updated paths to Room.
                         */
                        outcome.paths.forEach { path ->
                            roomRepository.persistPath(path)
                        }

                        /**
                         * Cache cleanup: remove local paths that no longer exist on the server.
                         * Keyed on what Firestore LISTED, not on what we managed to enrich — a
                         * path we failed to rebuild is still a path the shop has.
                         */
                        val cached = roomRepository.getPathsOnce()
                        cached.filter { it.id !in outcome.remoteIds }
                            .forEach { roomRepository.deletePath(it) }

                        /**
                         * A degraded fetch is not a synchronization: leaving the flag set is what
                         * makes the next load retry the paths that did not come back this time.
                         */
                        sharedDatastore.setShouldRefreshPaths(outcome.degraded)

                        /**
                         * Serve the paths Firestore still lists but that could not be enriched
                         * this time from the cache, so a single timeout does not make a tournée
                         * blink out of the UI. Cached rows are internally consistent — their
                         * `locations` were aligned with their cities by a previous successful
                         * fetch — which is why the rescue reads whole rows rather than patching
                         * the partial ones.
                         */
                        val enrichedIds = outcome.paths.map { it.id }.toSet()
                        val servedFromCache = cached.filter {
                            it.id in outcome.remoteIds && it.id !in enrichedIds
                        }

                        onSuccess(outcome.paths + servedFromCache)
                    }
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
                onSuccess.invoke(roomRepository.getPathsOnce())
            }
        }

    }
}
