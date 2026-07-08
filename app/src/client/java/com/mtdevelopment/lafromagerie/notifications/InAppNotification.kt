package com.mtdevelopment.lafromagerie.notifications

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/**
 * One entry of the client-side notification center (the bell button in the toolbar).
 *
 * Persisted as JSON in the "client_notifications" DataStore so pushes received while the
 * app was killed are still listed on next launch.
 */
@Keep
@Serializable
data class InAppNotification(
    val id: String,
    val title: String,
    val body: String,
    val timestampMillis: Long,
    val isRead: Boolean = false
)
