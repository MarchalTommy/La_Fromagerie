package com.mtdevelopment.lafromagerie.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val notificationStore: NotificationLocalStore
) : ViewModel() {

    val notifications: StateFlow<List<InAppNotification>> =
        notificationStore.notificationsFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val unreadCount: StateFlow<Int> =
        notificationStore.notificationsFlow.map { list -> list.count { !it.isRead } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markAllRead() {
        viewModelScope.launch { notificationStore.markAllRead() }
    }

    fun clearAll() {
        viewModelScope.launch { notificationStore.clearAll() }
    }
}
