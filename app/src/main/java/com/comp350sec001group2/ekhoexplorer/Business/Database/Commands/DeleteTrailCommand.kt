package com.comp350sec001group2.ekhoexplorer.Business.Database.Commands

import com.comp350sec001group2.ekhoexplorer.Business.Cache.ActiveUser
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PhotoFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailReviewFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailFields
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.tasks.await

class DeleteTrailCommand(
    private val trailId: String,
    private val user: ActiveUser,
) : FirestoreCommand<Unit>() {

    override suspend fun run(db: FirestoreContext) {
        val trailRef = db.document(TrailFields.COLLECTION, trailId)
        val trail = trailRef.get().await()
        if (!trail.exists()) throw FirestoreException(FirestoreErrorKind.NOT_FOUND, "Trail $trailId does not exist")
        if (!user.isAdmin && (user.userId.isEmpty() || trail.getString(TrailFields.OWNER_ID) != user.userId)) {
            throw FirestoreException(FirestoreErrorKind.PERMISSION_DENIED, "Only the author or an admin can delete a trail")
        }

        val reviews = db.collection(TrailFields.COLLECTION, trailId, TrailReviewFields.COLLECTION)
        for (review in reviews.get().await().documents) {
            purge(db, review.reference.collection(PhotoFields.COLLECTION))
        }
        purge(db, reviews)
        purge(db, db.collection(TrailFields.COLLECTION, trailId, PhotoFields.COLLECTION))
        purge(db, db.collection(TrailFields.COLLECTION, trailId, TrailFields.REVIEW_HEARTS))
        trailRef.delete().await()
    }

    private suspend fun purge(db: FirestoreContext, collection: CollectionReference) {
        while (true) {
            val docs = collection.limit(CHUNK).get().await().documents
            if (docs.isEmpty()) return
            val batch = db.firestore.batch()
            docs.forEach { batch.delete(it.reference) }
            batch.commit().await()
            if (docs.size < CHUNK) return
        }
    }

    private companion object { const val CHUNK = 400L }
}
