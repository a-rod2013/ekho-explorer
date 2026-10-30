package com.comp350sec001group2.ekhoexplorer.Business.Database.Queries

import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentReader
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestorePagedQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Comment
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.CommentFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.CommentMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.PageCursor
import com.google.firebase.firestore.Query

/** One page of a post's comments, oldest first. */
class GetCommentsPageQuery(
    private val postId: String,
    pageSize: Int = 20,
    after: PageCursor? = null,
) : FirestorePagedQuery<Comment>(pageSize, after) {

    override val reader: DocumentReader<Comment> get() = CommentMapper

    override fun baseQuery(db: FirestoreContext): Query =
        db.collection(PostFields.COLLECTION, postId, CommentFields.COLLECTION)
            .orderBy(CommentFields.CREATED_AT, Query.Direction.ASCENDING)
}
