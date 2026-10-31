package com.comp350sec001group2.ekhoexplorer.Business.Database.Commands

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.NotificationFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.NotificationType
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

/** Writes one notification into the recipient's inbox. Same [notificationId] again overwrites (and marks unread). */
class SendNotificationCommand(
    private val recipientId: String,
    private val notificationId: String,
    private val type: NotificationType,
    private val actorId: String,
    private val actorName: String,
    private val actorAvatarUrl: String?,
    private val postId: String,
    private val commentId: String?,
    private val preview: String,
) : FirestoreCommand<Unit>() {

    override suspend fun run(db: FirestoreContext) {
        val fields = mutableMapOf<String, Any?>(
            NotificationFields.TYPE to type.name,
            NotificationFields.ACTOR_ID to actorId,
            NotificationFields.ACTOR_NAME to actorName,
            NotificationFields.POST_ID to postId,
            NotificationFields.PREVIEW to preview.trim().take(NotificationFields.PREVIEW_LENGTH),
            NotificationFields.READ to false,
            NotificationFields.CREATED_AT to FieldValue.serverTimestamp(),
        )
        actorAvatarUrl?.let { fields[NotificationFields.ACTOR_AVATAR_URL] = it }
        commentId?.let { fields[NotificationFields.COMMENT_ID] = it }
        db.document(NotificationFields.USERS, recipientId, NotificationFields.COLLECTION, notificationId)
            .set(fields).await()
    }
}
