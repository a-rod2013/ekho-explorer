package com.comp350sec001group2.ekhoexplorer.Business.Notifications

import android.util.Log
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.RetractNotificationCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.SendNotificationCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreService
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.NotificationFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.NotificationType
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetPostAuthorQuery

/**
 * The one way to notify someone. Call it after the action that causes the notification succeeded.
 *
 *     notifications.send(NotificationType.POST_LIKE, postId = post.id, preview = post.caption, recipientId = post.userId)
 *     notifications.retract(NotificationType.POST_LIKE, postId = post.id, recipientId = post.userId)
 *
 * The actor is always the active user ([EhkoCache.activeUser]). The service
 *  - never notifies you about your own actions,
 *  - uses a fixed id per (type, post / comment, actor), so liking twice never makes two entries,
 *  - never throws: a failed notification is logged and dropped.
 *
 * To add a new kind of notification, add a [NotificationType] and an id rule in [idFor], then call [send].
 */
class NotificationService(private val service: FirestoreService) {

    /**
     * @param preview start of the post caption / comment text the notification is about.
     * @param commentId required for COMMENT_LIKE (the liked comment) and POST_COMMENT (the new comment).
     * @param recipientId who to notify. Leave null to notify the author of [postId] (costs one read).
     */
    suspend fun send(
        type: NotificationType,
        postId: String,
        preview: String = "",
        commentId: String? = null,
        recipientId: String? = null,
    ) {
        val actor = EhkoCache.activeUser
        if (!actor.isSignedIn) return
        try {
            val recipient = recipientId ?: service.execute(GetPostAuthorQuery(postId))
            if (recipient.isNullOrEmpty() || recipient == actor.userId) return
            service.execute(
                SendNotificationCommand(
                    recipientId = recipient,
                    notificationId = idFor(type, postId, commentId, actor.userId),
                    type = type,
                    actorId = actor.userId,
                    actorName = actor.userName,
                    actorAvatarUrl = actor.avatarUrl,
                    postId = postId,
                    commentId = commentId,
                    preview = preview,
                ),
            )
        } catch (e: FirestoreException) {
            Log.w(TAG, "Could not send $type notification: ${e.kind}", e)
        }
    }

    /** Takes back a notification, i.e. because the like that caused it was removed. */
    suspend fun retract(
        type: NotificationType,
        postId: String,
        commentId: String? = null,
        recipientId: String,
    ) {
        val actor = EhkoCache.activeUser
        if (!actor.isSignedIn || recipientId.isEmpty() || recipientId == actor.userId) return
        try {
            service.execute(RetractNotificationCommand(recipientId, idFor(type, postId, commentId, actor.userId)))
        } catch (e: FirestoreException) {
            Log.w(TAG, "Could not retract $type notification: ${e.kind}", e)
        }
    }

    private fun idFor(type: NotificationType, postId: String, commentId: String?, actorId: String): String =
        when (type) {
            NotificationType.POST_LIKE -> NotificationFields.postLikeId(postId, actorId)
            NotificationType.COMMENT_LIKE ->
                NotificationFields.commentLikeId(requireNotNull(commentId) { "COMMENT_LIKE needs a commentId" }, actorId)
            NotificationType.POST_COMMENT ->
                NotificationFields.postCommentId(requireNotNull(commentId) { "POST_COMMENT needs a commentId" })
        }

    private companion object { const val TAG = "NotificationService" }
}
