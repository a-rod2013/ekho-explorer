package com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories

import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreService
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Comment
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.NotificationType
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetCommentVotesQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Streams.ObserveCommentsStream
import com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions.AddCommentTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions.VoteOnCommentTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Notifications.NotificationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class FirestoreCommentRepository(
    private val service: FirestoreService,
    private val postId: String,
    private val notifications: NotificationService,
    private val onCommentAdded: () -> Unit = {},
) : CommentRepository {

    private val live = LiveCommentList(
        observe = { limit -> service.stream(ObserveCommentsStream(postId, limit)).map { it.value } },
        loadVotes = { ids -> service.execute(GetCommentVotesQuery(EhkoCache.activeUser.userId, ids)) },
        pageSize = PAGE_SIZE,
    )

    override val comments: Flow<List<Comment>> = combine(live.comments, EhkoCache.activeUserFlow) { list, user ->
        list.filterNot { user.isBlocked(it.userId) }
    }

    override val hasMore: Flow<Boolean> = live.hasMore
    override val isLoading: Flow<Boolean> = live.isLoading
    override val streamError: Flow<FirestoreErrorKind?> = live.error

    override suspend fun loadMore() = live.showMore()

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
        live.addLocal(created)
        onCommentAdded()
        notifications.send(NotificationType.POST_COMMENT, postId = postId, preview = created.text, commentId = id)
    }

    // Up or down vote: update the screen immediately, then confirm with the database.
    override suspend fun vote(commentId: String, tapped: Vote) {
        val original = live.current(commentId) ?: return
        live.beginVote(original, tapped)
        val result = try {
            service.execute(VoteOnCommentTransaction(postId, commentId, EhkoCache.activeUser.userId, tapped))
        } catch (e: FirestoreException) {
            live.cancelVote(commentId)
            throw e
        }
        live.confirmVote(commentId, result)

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

    private companion object { const val PAGE_SIZE = 20 }
}
