package com.comp350sec001group2.ekhoexplorer.ui.pages.notifications.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.AppNotification
import com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories.NotificationRepository
import com.comp350sec001group2.ekhoexplorer.Business.Settings.SettingsRepository
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the notifications page renders. */
data class NotificationsUIState(
    val notifications: List<AppNotification> = emptyList(),
    val notificationsEnabled: Boolean = true,
    val error: FirestoreErrorKind? = null,
) {
    val unreadCount: Int get() = notifications.count { !it.isRead }
}

class NotificationsViewModel(
    private val repository: NotificationRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val error = MutableStateFlow<FirestoreErrorKind?>(null)

    val uiState: StateFlow<NotificationsUIState> =
        combine(repository.notifications, EhkoCache.activeUserFlow, error) { items, user, e ->
            NotificationsUIState(
                notifications = items,
                notificationsEnabled = user.settings.notificationsEnabled,
                error = e,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsUIState())

    fun onMarkRead(notificationId: String) = launchGuarded { repository.markRead(notificationId) }
    fun onMarkAllRead() = launchGuarded { repository.markAllRead() }
    fun onSetEnabled(enabled: Boolean) = settings.setNotificationsEnabled(enabled)

    private fun launchGuarded(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                error.value = null
            } catch (e: FirestoreException) {
                error.value = e.kind
            }
        }
    }
}

val notificationsViewModelFactory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as EkhoApp
        NotificationsViewModel(app.container.notificationRepository, app.container.settingsRepository)
    }
}
