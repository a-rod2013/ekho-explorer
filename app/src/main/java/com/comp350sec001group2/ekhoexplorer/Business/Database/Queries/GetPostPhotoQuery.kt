package com.comp350sec001group2.ekhoexplorer.Business.Database.Queries

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PhotoFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostPhoto
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostPhotoMapper

/** Photo [index] of post [postId], or null if there is none. One read. */
class GetPostPhotoQuery(
    private val postId: String,
    private val index: Int,
) : FirestoreQuery<PostPhoto?>() {

    override suspend fun run(db: FirestoreContext): PostPhoto? =
        db.getDocument(
            db.document(PostFields.COLLECTION, postId, PhotoFields.COLLECTION, index.toString()),
            PostPhotoMapper,
        )
}
