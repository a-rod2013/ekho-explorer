package com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote
import com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories.FeedRepository
import com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories.NotificationRepository
import com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories.UserRelationsRepository
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedViewModel(
    private val repository: FeedRepository,
    private val relations: UserRelationsRepository,
    notifications: NotificationRepository,
) : ViewModel() {

    /** Unread notifications, for the bell badge. */
    val unreadNotifications: StateFlow<Int> =
        notifications.unreadCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val query = MutableStateFlow("")
    private val error = MutableStateFlow<FirestoreErrorKind?>(null)
    private val refreshing = MutableStateFlow(false)
    private val loading = MutableStateFlow(true)

    val uiState: StateFlow<FeedUIState> =
        combine(
            repository.posts,
            query,
            error,
            combine(loading, refreshing) { l, r -> l to r },
            EhkoCache.activeUserFlow,
        ) { posts, q, e, (l, r), user ->
            FeedUIState(
                posts = posts,
                query = q,
                error = e,
                isRefreshing = r,
                currentUserId = user.userId,
                isLoading = l,
                hasHiddenUsers = user.blockedUserIds.isNotEmpty() || user.hiddenUserIds.isNotEmpty(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedUIState())

    init { loadMore() }

    fun onQueryChange(newQuery: String) { query.value = newQuery }
    fun loadMore() = launchGuarded {
        try {
            repository.loadMore()
        } finally {
            loading.value = false
        }
    }
    fun onVote(postId: String, tapped: Vote) = launchGuarded { repository.vote(postId, tapped) }
    fun onAddPost(caption: String, imageUrls: List<String>) = launchGuarded { repository.addPost(caption, imageUrls) }
    fun onDeletePost(postId: String) = launchGuarded { repository.deletePost(postId) }
    fun onHideUser(userId: String) = launchGuarded { relations.hidePostsFrom(userId) }
    fun onBlockUser(userId: String) = launchGuarded { relations.block(userId) }

    /** Pull to refresh. [uiState].isRefreshing drives the spinner. */
    fun onRefresh() = launchGuarded {
        refreshing.value = true
        try {
            repository.refresh()
        } finally {
            refreshing.value = false
        }
    }

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

val feedViewModelFactory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as EkhoApp
        FeedViewModel(
            app.container.feedRepository,
            app.container.userRelationsRepository,
            app.container.notificationRepository,
        )
    }
}
