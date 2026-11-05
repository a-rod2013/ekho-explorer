package com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories

import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Comment
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote
import kotlinx.coroutines.flow.Flow

/** The comments of one post. */
interface CommentRepository {
    /** Loaded comments, oldest first, without comments from users the active user blocked. */
    val comments: Flow<List<Comment>>

    /** False once every comment has been loaded. */
    val hasMore: Flow<Boolean>

    suspend fun loadMore()
    suspend fun add(text: String)
    suspend fun vote(commentId: String, tapped: Vote)
}
