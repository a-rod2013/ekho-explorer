package com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.models

import kotlinx.coroutines.flow.Flow

/** Source of truth for all posts. */
interface PostRepository {
    val posts: Flow<List<Post>>

    fun vote(postId: String, tapped: Vote)
}
