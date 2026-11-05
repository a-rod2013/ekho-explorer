package com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Comment
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote
import com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories.CommentRepository
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the comments sheet renders. */
data class CommentsUIState(
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = true,
    val hasMore: Boolean = false,
    val error: FirestoreErrorKind? = null,
)

class CommentsViewModel(private val repository: CommentRepository) : ViewModel() {

    private val loading = MutableStateFlow(true)
    private val error = MutableStateFlow<FirestoreErrorKind?>(null)

    val uiState: StateFlow<CommentsUIState> =
        combine(repository.comments, repository.hasMore, loading, error) { comments, more, l, e ->
            CommentsUIState(comments = comments, isLoading = l, hasMore = more, error = e)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CommentsUIState())

    init { loadMore() }

    fun loadMore() = launchGuarded(showLoading = true) { repository.loadMore() }
    fun onSend(text: String) = launchGuarded { repository.add(text) }
    fun onVote(commentId: String, tapped: Vote) = launchGuarded { repository.vote(commentId, tapped) }

    private fun launchGuarded(showLoading: Boolean = false, block: suspend () -> Unit) {
        viewModelScope.launch {
            if (showLoading) loading.value = true
            try {
                block()
                error.value = null
            } catch (e: FirestoreException) {
                error.value = e.kind
            } finally {
                if (showLoading) loading.value = false
            }
        }
    }
}

fun commentsViewModelFactory(postId: String) = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as EkhoApp
        CommentsViewModel(app.container.commentRepositoryFor(postId))
    }
}
