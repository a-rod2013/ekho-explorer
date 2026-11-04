package com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories

import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreService
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Comment
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.NotificationType
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote
import com.comp350sec001group2.ekhoexplorer.Business.Database.Pager
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetCommentVotesQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetCommentsPageQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions.AddCommentTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions.VoteOnCommentTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Notifications.NotificationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

class FirestoreCommentRepository(
    private val service: FirestoreService,
    private val postId: String,
    private val notifications: NotificationService,
    private val onCommentAdded: () -> Unit = {},
) : CommentRepository {

    private val state = MutableStateFlow<List<Comment>>(emptyList())

    override val comments: Flow<List<Comment>> = combine(state, EhkoCache.activeUserFlow) { list, user ->
        list.filterNot { user.isBlocked(it.userId) }
    }

    private val more = MutableStateFlow(true)
    override val hasMore: Flow<Boolean> = more.asStateFlow()

    private val pager = Pager { cursor -> service.execute(GetCommentsPageQuery(postId, PAGE_SIZE, cursor)) }

    override suspend fun loadMore() {
        val page = pager.loadNext()
        more.value = pager.hasMore
        if (page.isEmpty()) return
        val votes = service.execute(GetCommentVotesQuery(EhkoCache.activeUser.userId, page.map { it.id }))
        val fresh = page.map { it.copy(myVote = votes[it.id] ?: Vote.NONE) }
        state.update { current ->
            val known = current.mapTo(HashSet()) { it.id }
            current + fresh.filter { it.id !in known }
        }
    }

    // Store the comment, then show it at the end of the list (newest is last).
    override suspend fun add(text: String) {
        val user = EhkoCache.activeUser
        val id = service.execute(AddCommentTransaction(postId, user.userId, user.userName, user.avatarUrl, text))
        val created = Comment(
            id = id,
            postId = postId,
            userId = user.userId,
            username = user.userName,
            avatarUrl = user.avatarUrl,
            text = text.trim(),
            upCount = 0,
            downCount = 0,
        )
        state.update { it + created }
        onCommentAdded()
        notifications.send(NotificationType.POST_COMMENT, postId = postId, preview = created.text, commentId = id)
    }

    // Up or down vote: update the screen immediately, then confirm with the database.
    override suspend fun vote(commentId: String, tapped: Vote) {
        val original = state.value.firstOrNull { it.id == commentId } ?: return
        replace(original.withVote(tapped))
        val result = try {
            service.execute(VoteOnCommentTransaction(postId, commentId, EhkoCache.activeUser.userId, tapped))
        } catch (e: FirestoreException) {
            replace(original)
            throw e
        }

        // Tell the comment's author about a new like, or take the notice back if the like was removed.
        if (result == Vote.UP) {
            notifications.send(
                NotificationType.COMMENT_LIKE,
                postId = postId,
                preview = original.text,
                commentId = commentId,
                recipientId = original.userId,
            )
        } else if (original.myVote == Vote.UP) {
            notifications.retract(
                NotificationType.COMMENT_LIKE,
                postId = postId,
                commentId = commentId,
                recipientId = original.userId,
            )
        }
    }

    private fun replace(comment: Comment) =
        state.update { list -> list.map { if (it.id == comment.id) comment else it } }

    // Stays under the 30-id limit of GetCommentVotesQuery.
    private companion object { const val PAGE_SIZE = 20 }
}
