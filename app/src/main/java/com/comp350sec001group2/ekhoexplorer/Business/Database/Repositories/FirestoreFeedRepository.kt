package com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories

import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.AddFeedPostCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreService
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Post
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote
import com.comp350sec001group2.ekhoexplorer.Business.Database.Pager
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetFeedPageQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetFeedVotesQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions.VoteOnFeedPostTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FirestoreFeedRepository(
    private val service: FirestoreService,
    private val currentUserId: () -> String,
    private val currentUsername: () -> String,
    private val currentAvatarUrl: () -> String?,
) : FeedRepository {

    private val state = MutableStateFlow<List<Post>>(emptyList())
    override val posts: Flow<List<Post>> = state.asStateFlow()

    private val pager = Pager { cursor -> service.execute(GetFeedPageQuery(PAGE_SIZE, cursor)) }

    // Get posts: load the next page, then mark which ones I have voted on.
    override suspend fun loadMore() {
        val page = pager.loadNext()
        if (page.isEmpty()) return
        val votes = service.execute(GetFeedVotesQuery(currentUserId(), page.map { it.id }))
        state.update { current -> current + page.map { it.copy(myVote = votes[it.id] ?: Vote.NONE) } }
    }

    override suspend fun refresh() {
        pager.reset()
        state.value = emptyList()
        loadMore()
    }

    // Like or dislike: update the screen immediately, then confirm with the database.
    override suspend fun vote(postId: String, tapped: Vote) {
        val original = state.value.firstOrNull { it.id == postId } ?: return
        replace(original.withVote(tapped))
        try {
            service.execute(VoteOnFeedPostTransaction(postId, currentUserId(), tapped))
        } catch (e: FirestoreException) {
            replace(original)          // undo just this post
            throw e
        }
    }

    // Add a post, then reload so it appears at the top.
    override suspend fun addPost(caption: String, imageUrls: List<String>) {
        service.execute(
            AddFeedPostCommand(currentUserId(), currentUsername(), currentAvatarUrl(), imageUrls, caption),
        )
        refresh()
    }

    private fun replace(post: Post) =
        state.update { list -> list.map { if (it.id == post.id) post else it } }

    private companion object { const val PAGE_SIZE = 10 }
}
