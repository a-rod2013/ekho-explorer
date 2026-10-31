package com.comp350sec001group2.ekhoexplorer.Business.Database.Models

import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentData
import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentReader

enum class NotificationType {
    /** Someone liked a post the user made. */
    POST_LIKE,

    /** Someone liked a comment the user made. */
    COMMENT_LIKE,

    /** Someone commented on a post the user made. */
    POST_COMMENT,
}


data class AppNotification(
    val id: String,
    val type: NotificationType,
    val actorId: String,
    val actorName: String,
    val actorAvatarUrl: String?,
    val postId: String,
    /** Set for COMMENT_LIKE and POST_COMMENT. */
    val commentId: String?,
    /** Start of the post caption / comment the notification is about. May be empty. */
    val preview: String,
    val isRead: Boolean,
    /** Epoch millis. 0 while the server has not stamped it yet. */
    val createdAtMillis: Long,
)

object NotificationFields {
    const val USERS = "users"
    const val COLLECTION = "notifications"
    const val TYPE = "type"
    const val ACTOR_ID = "actorId"
    const val ACTOR_NAME = "actorName"
    const val ACTOR_AVATAR_URL = "actorAvatarUrl"
    const val POST_ID = "postId"
    const val COMMENT_ID = "commentId"
    const val PREVIEW = "preview"
    const val READ = "read"
    const val CREATED_AT = "createdAt"

    const val PREVIEW_LENGTH = 80

    // Deterministic ids: liking again overwrites the same entry instead of piling up duplicates,
    // and un-liking can delete it.
    fun postLikeId(postId: String, actorId: String) = "postlike_${postId}_$actorId"
    fun commentLikeId(commentId: String, actorId: String) = "commentlike_${commentId}_$actorId"
    fun postCommentId(commentId: String) = "comment_$commentId"
}

object NotificationMapper : DocumentReader<AppNotification> {
    override fun read(doc: DocumentData) = AppNotification(
        id = doc.id,
        type = doc.enum<NotificationType>(NotificationFields.TYPE),
        actorId = doc.string(NotificationFields.ACTOR_ID),
        actorName = doc.string(NotificationFields.ACTOR_NAME),
        actorAvatarUrl = doc.stringOrNull(NotificationFields.ACTOR_AVATAR_URL),
        postId = doc.string(NotificationFields.POST_ID),
        commentId = doc.stringOrNull(NotificationFields.COMMENT_ID),
        preview = doc.stringOrNull(NotificationFields.PREVIEW) ?: "",
        isRead = doc.booleanOrNull(NotificationFields.READ) ?: false,
        createdAtMillis = doc.instantOrNull(NotificationFields.CREATED_AT)?.toEpochMilli() ?: 0L,
    )
}
