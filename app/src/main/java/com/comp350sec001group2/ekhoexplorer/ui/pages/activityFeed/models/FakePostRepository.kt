package com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.models

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakePostRepository(initial: List<Post> = SamplePosts.all): PostRepository {
    private val state = MutableStateFlow(initial)

    override val posts: Flow<List<Post>> = state.asStateFlow()

    override fun vote(postId: String, tapped: Vote) {
        state.update { list -> list.map {if(it.id == postId) it.withVote(tapped) else it} }
    }
}
