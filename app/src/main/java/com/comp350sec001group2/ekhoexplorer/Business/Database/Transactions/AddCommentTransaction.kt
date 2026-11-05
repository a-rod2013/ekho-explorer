package com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.CommentFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.TransactionScope
import com.google.firebase.firestore.FieldValue

/** Adds a comment and bumps the post's comment count together. Returns the new comment's id. */
class AddCommentTransaction(
    private val postId: String,
    private val userId: String,
    private val username: String,
    private val avatarUrl: String?,
    private val text: String,
) : FirestoreTransaction<String>() {

    override fun run(tx: TransactionScope): String {
        val body = text.trim()
        if (body.isEmpty()) throw FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, "A comment cannot be empty")
        if (body.length > CommentFields.MAX_TEXT_LENGTH) {
            throw FirestoreException(
                FirestoreErrorKind.INVALID_ARGUMENT,
                "A comment holds at most ${CommentFields.MAX_TEXT_LENGTH} characters",
            )
        }

        val postRef = tx.db.document(PostFields.COLLECTION, postId)
        if (tx.read(postRef, PostMapper) == null) {                          // reads first
            throw FirestoreException(FirestoreErrorKind.NOT_FOUND, "Post $postId does not exist")
        }
        val commentRef = tx.db.collection(PostFields.COLLECTION, postId, CommentFields.COLLECTION).document()

        val fields = mutableMapOf<String, Any?>(                             // then writes
            CommentFields.POST_ID to postId,
            CommentFields.USER_ID to userId,
            CommentFields.USERNAME to username,
            CommentFields.TEXT to body,
            CommentFields.UP_COUNT to 0L,
            CommentFields.DOWN_COUNT to 0L,
            CommentFields.CREATED_AT to FieldValue.serverTimestamp(),
        )
        avatarUrl?.let { fields[CommentFields.AVATAR_URL] = it }
        tx.set(commentRef, fields)
        tx.update(postRef, mapOf(PostFields.COMMENT_COUNT to FieldValue.increment(1)))
        return commentRef.id
    }
}
