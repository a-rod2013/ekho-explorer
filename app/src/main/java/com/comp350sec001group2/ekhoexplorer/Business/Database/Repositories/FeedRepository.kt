package com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories

import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Post
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    /** The loaded posts, already without posts from users the active user blocked or hid. */
    val posts: Flow<List<Post>>
    suspend fun loadMore()
    suspend fun refresh()
    suspend fun vote(postId: String, tapped: Vote)
    suspend fun addPost(caption: String, imageUrls: List<String>)

    /** Deletes the post for good. Only its author can do this. */
    suspend fun deletePost(postId: String)

    /** Tells the feed a comment was added to [postId], so the shown count stays right. */
    fun onCommentAdded(postId: String)
}

