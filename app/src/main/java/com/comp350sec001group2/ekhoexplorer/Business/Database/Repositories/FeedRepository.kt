package com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories

import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Post
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    val posts: Flow<List<Post>>
    suspend fun loadMore()
    suspend fun refresh()
    suspend fun vote(postId: String, tapped: Vote)
    suspend fun addPost(caption: String, imageUrls: List<String>)
}
