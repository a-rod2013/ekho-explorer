package com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.VoteMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.TransactionScope
import com.google.firebase.firestore.FieldValue

class VoteOnFeedPostTransaction(
    private val postId: String,
    private val userId: String,
    private val tapped: Vote,
) : FirestoreTransaction<Vote>() {

    override fun run(tx: TransactionScope): Vote {
        val voteRef = tx.db.document("votes", "${postId}_$userId")
        val postRef = tx.db.document(PostFields.COLLECTION, postId)

        val previous = tx.read(voteRef, VoteMapper)?.value ?: Vote.NONE   // reads first
        val next = VoteMath.next(previous, tapped)
        val (upChange, downChange) = VoteMath.deltas(previous, next)

        if (next == Vote.NONE) {
            tx.delete(voteRef)
        } else {
            tx.set(voteRef, mapOf("postId" to postId, "userId" to userId, "value" to next.name))
        }
        tx.update(                                                         // then writes
            postRef,
            mapOf(
                PostFields.UP_COUNT to FieldValue.increment(upChange.toLong()),
                PostFields.DOWN_COUNT to FieldValue.increment(downChange.toLong()),
            ),
        )
        return next
    }
}
