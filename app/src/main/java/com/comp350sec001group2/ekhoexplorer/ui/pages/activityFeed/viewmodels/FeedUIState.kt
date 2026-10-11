package com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.viewmodels

import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.models.Post

/** Everything the feed renders. */
data class FeedUIState(
    val posts: List<Post> = emptyList(),
    val query: String="",
)
