package com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote
import com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories.FeedRepository
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedViewModel(private val repository: FeedRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val error = MutableStateFlow<FirestoreErrorKind?>(null)

    val uiState: StateFlow<FeedUIState> = combine(repository.posts, query, error) { posts, q, e ->
        FeedUIState(posts = posts, query = q, error = e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedUIState())

    init { loadMore() }

    fun onQueryChange(newQuery: String) { query.value = newQuery }
    fun loadMore() = launchGuarded { repository.loadMore() }
    fun onVote(postId: String, tapped: Vote) = launchGuarded { repository.vote(postId, tapped) }
    fun onAddPost(caption: String, imageUrls: List<String>) = launchGuarded { repository.addPost(caption, imageUrls) }

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
        FeedViewModel(app.container.feedRepository)
    }
}
