package com.comp350sec001group2.ekhoexplorer.Business.Database.Queries

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestorePaths
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.VoteMapper
import com.google.firebase.firestore.FieldPath


class GetFeedVotesQuery(
    private val userId: String,
    private val postIds: List<String>,
) : FirestoreQuery<Map<String, Vote>>() {

    override suspend fun run(db: FirestoreContext): Map<String, Vote> {
        if (postIds.isEmpty()) return emptyMap()
        if (postIds.size > 30) {
            throw FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, "At most 30 posts per call")
        }
        val ids = postIds.map { FirestorePaths.requireValidSegment("${it}_$userId") }
        val query = db.collection("votes").whereIn(FieldPath.documentId(), ids)
        return db.getDocuments(query, ids.size, VoteMapper).associate { it.postId to it.value }
    }
}
