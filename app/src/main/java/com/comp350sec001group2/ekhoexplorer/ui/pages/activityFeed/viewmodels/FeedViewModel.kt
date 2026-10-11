package com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.models.FakePostRepository
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.models.PostRepository
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.models.Vote

class FeedViewModel(
    private val repository: PostRepository = FakePostRepository(),
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<FeedUIState> = combine(repository.posts, query) { posts, query ->
        FeedUIState(posts = posts, query = query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedUIState())

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun onVote(postId: String, tapped: Vote) {
        repository.vote(postId, tapped)
    }
}
