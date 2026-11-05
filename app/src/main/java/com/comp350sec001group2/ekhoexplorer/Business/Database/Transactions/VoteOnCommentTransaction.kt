package com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.CommentFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.CommentVoteFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.CommentVoteMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote
import com.comp350sec001group2.ekhoexplorer.Business.Database.TransactionScope
import com.google.firebase.firestore.FieldValue

/** Same rules as [VoteOnFeedPostTransaction]: tapping the active vote clears it. */
class VoteOnCommentTransaction(
    private val postId: String,
    private val commentId: String,
    private val userId: String,
    private val tapped: Vote,
) : FirestoreTransaction<Vote>() {

    override fun run(tx: TransactionScope): Vote {
        val voteRef = tx.db.document(CommentVoteFields.COLLECTION, "${commentId}_$userId")
        val commentRef = tx.db.document(PostFields.COLLECTION, postId, CommentFields.COLLECTION, commentId)

        val previous = tx.read(voteRef, CommentVoteMapper)?.value ?: Vote.NONE   // reads first
        val next = VoteMath.next(previous, tapped)
        val (upChange, downChange) = VoteMath.deltas(previous, next)

        if (next == Vote.NONE) {
            tx.delete(voteRef)
        } else {
            tx.set(
                voteRef,
                mapOf(
                    CommentVoteFields.COMMENT_ID to commentId,
                    CommentVoteFields.POST_ID to postId,
                    CommentVoteFields.USER_ID to userId,
                    CommentVoteFields.VALUE to next.name,
                ),
            )
        }
        tx.update(                                                                // then writes
            commentRef,
            mapOf(
                CommentFields.UP_COUNT to FieldValue.increment(upChange.toLong()),
                CommentFields.DOWN_COUNT to FieldValue.increment(downChange.toLong()),
            ),
        )
        return next
    }
}
