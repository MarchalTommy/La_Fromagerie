package com.mtdevelopment.delivery.domain.repository

import com.mtdevelopment.delivery.domain.model.DeliveryPath

/**
 * What one delivery-path fetch actually established, with the two facts kept apart.
 *
 * Reading a path takes two steps that fail independently: Firestore lists the documents, then each
 * document is enriched with the geographic center of every city it covers. Collapsing both into a
 * plain `List<DeliveryPath>` loses the distinction between "the shop deleted this tournée" and "one
 * of its twenty cities timed out just now" — and the cache cleanup, which may only act on the
 * former, was deciding on the latter. A single timeout on the biggest path therefore deleted it
 * from the local cache and marked the cache synchronized, so it stayed gone until the remote
 * `path_timestamp` moved again. That is the "the Friday card disappears sometimes" bug.
 *
 * @property paths Paths that came back fully enriched. May be a strict subset of [remoteIds].
 * @property remoteIds Every document id the `delivery_paths` collection listed, enriched or not.
 *   **The only legitimate basis for deleting a row from the local cache.**
 * @property degraded At least one listed document could not be enriched, so this fetch is not a
 *   complete picture and must not mark the cache as synchronized.
 */
data class PathFetchOutcome(
    val paths: List<DeliveryPath>,
    val remoteIds: Set<String>,
    val degraded: Boolean
)

interface FirestorePathRepository {

    fun getAllDeliveryPaths(
        withGeoJson: Boolean = false,
        onSuccess: (PathFetchOutcome) -> Unit,
        onFailure: () -> Unit
    )

    fun getDeliveryPath(
        pathName: String,
        onSuccess: (DeliveryPath?) -> Unit,
        onFailure: () -> Unit
    )

}
