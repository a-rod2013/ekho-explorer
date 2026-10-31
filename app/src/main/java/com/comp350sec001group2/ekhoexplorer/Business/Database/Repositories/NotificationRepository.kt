package com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories

import android.util.Log
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Database.Batches.MarkNotificationsReadBatch
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreService
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.AppNotification
import com.comp350sec001group2.ekhoexplorer.Business.Database.Streams.ObserveNotificationsStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

/** The active user's notification inbox (reading it; sending is [NotificationService]). */
interface NotificationRepository {
    /** Newest first. Empty when signed out or when the user turned notifications off. */
    val notifications: Flow<List<AppNotification>>

    /** Unread entries among the newest 50. 0 when notifications are off. */
    val unreadCount: Flow<Int>

    suspend fun markRead(notificationId: String)
    suspend fun markAllRead()
}

class FirestoreNotificationRepository(
    private val service: FirestoreService,
    scope: CoroutineScope,
) : NotificationRepository {

    // One shared listener for the bell badge and the page. It follows the active user and
    // switches off completely (no reads) while notifications are disabled.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val inbox: Flow<List<AppNotification>> = EhkoCache.activeUserFlow
        .map { it.userId to it.settings.notificationsEnabled }
        .distinctUntilChanged()
        .flatMapLatest { (userId, enabled) ->
            if (userId.isEmpty() || !enabled) {
                flowOf(emptyList())
            } else {
                service.stream(ObserveNotificationsStream(userId))
                    .map { it.value }
                    .catch { e ->
                        Log.w("NotificationRepository", "Inbox stream failed", e)
                        emit(emptyList())
                    }
            }
        }
        .shareIn(scope, SharingStarted.WhileSubscribed(STOP_DELAY_MILLIS), replay = 1)

    override val notifications: Flow<List<AppNotification>> = inbox
    override val unreadCount: Flow<Int> = inbox.map { list -> list.count { !it.isRead } }.distinctUntilChanged()

    override suspend fun markRead(notificationId: String) {
        val userId = EhkoCache.activeUser.userId
        if (userId.isEmpty()) return
        service.execute(MarkNotificationsReadBatch(userId, listOf(notificationId)))
    }

    override suspend fun markAllRead() {
        val userId = EhkoCache.activeUser.userId
        if (userId.isEmpty()) return
        val unread = inbox.first().filter { !it.isRead }.map { it.id }
        if (unread.isEmpty()) return
        service.execute(MarkNotificationsReadBatch(userId, unread))
    }

    private companion object { const val STOP_DELAY_MILLIS = 5_000L }
}
