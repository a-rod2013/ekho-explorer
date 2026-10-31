package com.comp350sec001group2.ekhoexplorer.Business.Database.Batches

import com.comp350sec001group2.ekhoexplorer.Business.Database.BatchScope
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreBatch
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.NotificationFields

/** Marks the given notifications in [userId]'s inbox as read. */
class MarkNotificationsReadBatch(
    private val userId: String,
    private val notificationIds: List<String>,
) : FirestoreBatch() {

    override fun stage(batch: BatchScope) {
        notificationIds.forEach { id ->
            batch.update(
                batch.db.document(NotificationFields.USERS, userId, NotificationFields.COLLECTION, id),
                mapOf(NotificationFields.READ to true),
            )
        }
    }
}
