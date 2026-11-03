package com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories


import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.AddFeedPostCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.DeleteFeedPostCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreService
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.NotificationType
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Post
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote
import com.comp350sec001group2.ekhoexplorer.Business.Database.Pager
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetFeedPageQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetFeedVotesQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions.VoteOnFeedPostTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Notifications.NotificationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

/** The signed-in user comes from [EhkoCache.activeUser]. */
class FirestoreFeedRepository(
    private val service: FirestoreService,
    private val notifications: NotificationService,
) : FeedRepository {

    private val state = MutableStateFlow<List<Post>>(emptyList())

    // Filtering lives here (not in the loaded list) so blocking someone hides their posts instantly.
    override val posts: Flow<List<Post>> = combine(state, EhkoCache.activeUserFlow) { list, user ->
        list.filterNot { user.hidesPostsFrom(it.userId) }
    }

    private val pager = Pager { cursor -> service.execute(GetFeedPageQuery(PAGE_SIZE, cursor)) }

    override suspend fun loadMore() {
        loadNextPage()
        keepLoadingWhileFeedLooksEmpty()
    }

    // Get posts: load the next page, then mark which ones I have voted on.
    private suspend fun loadNextPage() {
        val page = pager.loadNext()
        if (page.isEmpty()) return
        val fresh = withMyVotes(page)
        state.update { current ->
            val known = current.mapTo(HashSet()) { it.id }
            current + fresh.filter { it.id !in known }
        }
    }

    // Reload the first page. The old list stays on screen until the new one arrives.
    override suspend fun refresh() {
        pager.reset()
        state.value = withMyVotes(pager.loadNext())
        keepLoadingWhileFeedLooksEmpty()
    }

    // If every loaded post is from a hidden or blocked account, look further before the screen
    // says there are no posts. Gives up after a few pages.
    private suspend fun keepLoadingWhileFeedLooksEmpty() {
        var extraPages = 0
        while (pager.hasMore && extraPages < MAX_EXTRA_PAGES && isVisibleFeedEmpty()) {
            loadNextPage()
            extraPages++
        }
    }

    private fun isVisibleFeedEmpty(): Boolean {
        val user = EhkoCache.activeUser
        return state.value.none { !user.hidesPostsFrom(it.userId) }
    }

    // Like or dislike: update the screen immediately, then confirm with the database.
    override suspend fun vote(postId: String, tapped: Vote) {
        val original = state.value.firstOrNull { it.id == postId } ?: return
        replace(original.withVote(tapped))
        val result = try {
            service.execute(VoteOnFeedPostTransaction(postId, EhkoCache.activeUser.userId, tapped))
        } catch (e: FirestoreException) {
            replace(original)          // undo just this post
            throw e
        }

        // Tell the author about a new like, or take the notice back if the like was removed.
        if (result == Vote.UP) {
            notifications.send(
                NotificationType.POST_LIKE,
                postId = postId,
                preview = original.caption,
                recipientId = original.userId,
            )
        } else if (original.myVote == Vote.UP) {
            notifications.retract(NotificationType.POST_LIKE, postId = postId, recipientId = original.userId)
        }
    }

    // Add a post, then reload so it appears at the top.
    override suspend fun addPost(caption: String, imageUrls: List<String>) {
        val user = EhkoCache.activeUser
        service.execute(AddFeedPostCommand(user.userId, user.userName, user.avatarUrl, imageUrls, caption))
        refresh()
    }

    // Delete in the database first, then drop it from the screen.
    override suspend fun deletePost(postId: String) {
        service.execute(DeleteFeedPostCommand(postId, EhkoCache.activeUser.userId))
        state.update { list -> list.filterNot { it.id == postId } }
    }

    override fun onCommentAdded(postId: String) {
        state.update { list -> list.map { if (it.id == postId) it.copy(commentCount = it.commentCount + 1) else it } }
    }

    private suspend fun withMyVotes(page: List<Post>): List<Post> {
        if (page.isEmpty()) return page
        val votes = service.execute(GetFeedVotesQuery(EhkoCache.activeUser.userId, page.map { it.id }))
        return page.map { it.copy(myVote = votes[it.id] ?: Vote.NONE) }
    }

    private fun replace(post: Post) =
        state.update { list -> list.map { if (it.id == post.id) post else it } }

    private companion object {
        const val PAGE_SIZE = 10
        const val MAX_EXTRA_PAGES = 5
    }
}
