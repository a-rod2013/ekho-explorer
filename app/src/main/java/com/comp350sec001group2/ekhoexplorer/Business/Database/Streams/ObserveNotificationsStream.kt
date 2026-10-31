package com.comp350sec001group2.ekhoexplorer.Business.Database.Streams

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreStream
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.AppNotification
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.NotificationFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.NotificationMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.StreamUpdate
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow

/** Live list of the newest [limit] notifications in [userId]'s inbox. One listener feeds the bell and the page. */
class ObserveNotificationsStream(
    private val userId: String,
    private val limit: Int = 50,
) : FirestoreStream<StreamUpdate<List<AppNotification>>>() {

    override fun observe(db: FirestoreContext): Flow<StreamUpdate<List<AppNotification>>> =
        db.observeQuery(
            query = db.collection(NotificationFields.USERS, userId, NotificationFields.COLLECTION)
                .orderBy(NotificationFields.CREATED_AT, Query.Direction.DESCENDING),
            limit = limit,
            reader = NotificationMapper,
        )
}
