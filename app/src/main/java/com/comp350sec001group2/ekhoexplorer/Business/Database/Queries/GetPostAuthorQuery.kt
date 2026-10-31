package com.comp350sec001group2.ekhoexplorer.Business.Database.Queries

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostFields
import kotlinx.coroutines.tasks.await

/** The userId of a post's author, or null if the post is gone or has no author recorded. */
class GetPostAuthorQuery(private val postId: String) : FirestoreQuery<String?>() {

    override suspend fun run(db: FirestoreContext): String? =
        db.document(PostFields.COLLECTION, postId).get().await().getString(PostFields.USER_ID)
}
