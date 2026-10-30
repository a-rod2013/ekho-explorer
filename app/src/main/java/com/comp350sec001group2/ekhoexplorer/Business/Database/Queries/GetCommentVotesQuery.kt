package com.comp350sec001group2.ekhoexplorer.Business.Database.Queries

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestorePaths
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.CommentVoteFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.CommentVoteMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote
import com.google.firebase.firestore.FieldPath

/** The votes [userId] has cast on [commentIds], keyed by comment id. */
class GetCommentVotesQuery(
    private val userId: String,
    private val commentIds: List<String>,
) : FirestoreQuery<Map<String, Vote>>() {

    override suspend fun run(db: FirestoreContext): Map<String, Vote> {
        if (commentIds.isEmpty()) return emptyMap()
        if (commentIds.size > 30) {
            throw FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, "At most 30 comments per call")
        }
        val ids = commentIds.map { FirestorePaths.requireValidSegment("${it}_$userId") }
        val query = db.collection(CommentVoteFields.COLLECTION).whereIn(FieldPath.documentId(), ids)
        return db.getDocuments(query, ids.size, CommentVoteMapper).associate { it.commentId to it.value }
    }
}
