package com.comp350sec001group2.ekhoexplorer.Business.Database.Commands

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.NotificationFields
import kotlinx.coroutines.tasks.await

/** Removes a notification from the recipient's inbox. Does nothing if it is already gone. */
class RetractNotificationCommand(
    private val recipientId: String,
    private val notificationId: String,
) : FirestoreCommand<Unit>() {

    override suspend fun run(db: FirestoreContext) {
        db.document(NotificationFields.USERS, recipientId, NotificationFields.COLLECTION, notificationId)
            .delete().await()
    }
}
