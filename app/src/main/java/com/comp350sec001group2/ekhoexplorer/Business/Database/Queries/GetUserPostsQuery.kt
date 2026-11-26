package com.comp350sec001group2.ekhoexplorer.Business.Database.Queries

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Post
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostMapper
import com.google.firebase.firestore.Query

/**
 * Retrieves posts created by a specific user, ordered newest first.
 */
class GetUserPostsQuery(
    private val userId: String
) : FirestoreQuery<List<Post>>() {

    override suspend fun run(db: FirestoreContext): List<Post> {
        require(userId.isNotBlank())

        val posts = try {
            val query = db.collection(PostFields.COLLECTION)
                .whereEqualTo(PostFields.USER_ID, userId)
                .orderBy(PostFields.CREATED_AT, Query.Direction.DESCENDING)

            db.getDocuments(
                query = query,
                limit = 100,
                reader = PostMapper
            )
        } catch (e: Exception) {
            val query = db.collection(PostFields.COLLECTION)
                .whereEqualTo(PostFields.USER_ID, userId)

            db.getDocuments(
                query = query,
                limit = 100,
                reader = PostMapper
            )
        }

        return posts.sortedWith(
            compareByDescending<Post> { it.createdAtMillis }.thenByDescending { it.id }
        )
    }
}
