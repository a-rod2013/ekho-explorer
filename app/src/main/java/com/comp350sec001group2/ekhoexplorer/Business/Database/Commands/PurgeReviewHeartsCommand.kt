package com.comp350sec001group2.ekhoexplorer.Business.Database.Commands

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailReviewHeartFields
import kotlinx.coroutines.tasks.await

/**
 * Removes every heart given to one review.
 */
class PurgeReviewHeartsCommand(
    private val trailId: String,
    private val reviewId: String,
) : FirestoreCommand<Unit>() {

    override suspend fun run(db: FirestoreContext) {
        val hearts = db.collection(TrailFields.COLLECTION, trailId, TrailFields.REVIEW_HEARTS)
            .whereEqualTo(TrailReviewHeartFields.REVIEW_ID, reviewId)
        while (true) {
            val docs = hearts.limit(CHUNK).get().await().documents
            if (docs.isEmpty()) return
            val batch = db.firestore.batch()
            docs.forEach { batch.delete(it.reference) }
            batch.commit().await()
            if (docs.size < CHUNK) return
        }
    }

    private companion object { const val CHUNK = 400L }
}
