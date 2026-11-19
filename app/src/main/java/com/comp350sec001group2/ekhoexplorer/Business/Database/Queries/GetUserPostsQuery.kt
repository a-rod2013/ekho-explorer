package com.comp350sec001group2.ekhoexplorer.Business.Database.Queries

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Post
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostMapper

/**
 * Retrieves posts created by a specific user.
 */
class GetUserPostsQuery(
    private val userId: String
) : FirestoreQuery<List<Post>>() {

    override suspend fun run(db: FirestoreContext): List<Post> {
        require(userId.isNotBlank())

        val query = db.collection(PostFields.COLLECTION)
            .whereEqualTo(PostFields.USER_ID, userId)

        return db.getDocuments(
            query = query,
            limit = 100,
            reader = PostMapper
        )
    }
}
