package com.comp350sec001group2.ekhoexplorer.Business.Database.Streams

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreStream
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Comment
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.CommentFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.CommentMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.StreamUpdate
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow

class ObserveCommentsStream(
    private val postId: String,
    private val limit: Int,
) : FirestoreStream<StreamUpdate<List<Comment>>>() {

    override fun observe(db: FirestoreContext): Flow<StreamUpdate<List<Comment>>> =
        db.observeQuery(
            query = db.collection(PostFields.COLLECTION, postId, CommentFields.COLLECTION)
                .orderBy(CommentFields.CREATED_AT, Query.Direction.ASCENDING),
            limit = limit,
            reader = CommentMapper,
        )
}
