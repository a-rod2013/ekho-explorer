package com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.viewmodels

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Post


/** Everything the feed renders. */
data class FeedUIState(
    val posts: List<Post> = emptyList(),
    val error: FirestoreErrorKind? = null,
    val query: String="",
)
