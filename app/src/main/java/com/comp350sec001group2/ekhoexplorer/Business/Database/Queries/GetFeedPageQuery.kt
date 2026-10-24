package com.comp350sec001group2.ekhoexplorer.Business.Database.Queries

import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentReader
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestorePagedQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Post
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.PageCursor



import com.google.firebase.firestore.Query

class GetFeedPageQuery(
    pageSize: Int = 10,
    after: PageCursor? = null,
) : FirestorePagedQuery<Post>(pageSize, after) {

    override val reader: DocumentReader<Post> get() = PostMapper

    override fun baseQuery(db: FirestoreContext): Query =
        db.collection(PostFields.COLLECTION)
            .orderBy(PostFields.CREATED_AT, Query.Direction.DESCENDING)
}
