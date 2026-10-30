package com.comp350sec001group2.ekhoexplorer.Business.Database.Commands

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.CommentFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.CommentVoteFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostFields
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Deletes a post and everything hanging off it: its comments, the votes on the post and the
 * votes on its comments. Only the post's author may do this.
 */
class DeleteFeedPostCommand(
    private val postId: String,
    private val userId: String,
) : FirestoreCommand<Unit>() {

    override suspend fun run(db: FirestoreContext) {
        val postRef = db.document(PostFields.COLLECTION, postId)
        val post = postRef.get().await()
        if (!post.exists()) throw FirestoreException(FirestoreErrorKind.NOT_FOUND, "Post $postId does not exist")
        if (userId.isEmpty() || post.getString(PostFields.USER_ID) != userId) {
            throw FirestoreException(FirestoreErrorKind.PERMISSION_DENIED, "Only the author can delete a post")
        }

        purge(db, db.collection(PostFields.COLLECTION, postId, CommentFields.COLLECTION))
        purge(db, db.collection(CommentVoteFields.COLLECTION).whereEqualTo(CommentVoteFields.POST_ID, postId))
        purge(db, db.collection("votes").whereEqualTo("postId", postId))
        postRef.delete().await()
    }

    /** Deletes every document the query matches, [CHUNK] at a time (a batch holds at most 500). */
    private suspend fun purge(db: FirestoreContext, query: Query) {
        while (true) {
            val docs = query.limit(CHUNK).get().await().documents
            if (docs.isEmpty()) return
            val batch = db.firestore.batch()
            docs.forEach { batch.delete(it.reference) }
            batch.commit().await()
            if (docs.size < CHUNK) return
        }
    }

    private companion object { const val CHUNK = 400L }
}
