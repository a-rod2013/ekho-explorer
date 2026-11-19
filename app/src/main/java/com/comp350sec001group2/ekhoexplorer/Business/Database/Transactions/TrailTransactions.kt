package com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions

import com.comp350sec001group2.ekhoexplorer.Business.Cache.ActiveUser
import com.comp350sec001group2.ekhoexplorer.Business.Database.Batches.photoFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PhotoFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PresenceReader
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailReviewFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailReviewHeartFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailReviewMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.TransactionScope
import com.comp350sec001group2.ekhoexplorer.Business.Photos.EncodedPhoto
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoLimits
import com.comp350sec001group2.ekhoexplorer.Business.Photos.requireValidForPost
import com.comp350sec001group2.ekhoexplorer.Business.Trails.RatingMath
import com.comp350sec001group2.ekhoexplorer.Business.Trails.Trail
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailInput
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailLimits
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailRules
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailValidator
import com.comp350sec001group2.ekhoexplorer.Business.Trails.toViewer
import com.google.firebase.firestore.FieldValue

private fun invalid(detail: String) = FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, detail)
private fun denied(detail: String) = FirestoreException(FirestoreErrorKind.PERMISSION_DENIED, detail)
private fun missing(what: String) = FirestoreException(FirestoreErrorKind.NOT_FOUND, "$what does not exist")

private fun TransactionScope.trailRef(trailId: String) = db.document(TrailFields.COLLECTION, trailId)

private fun TransactionScope.readTrail(trailId: String): Trail =
    read(trailRef(trailId), TrailMapper) ?: throw missing("Trail $trailId")

private fun TransactionScope.trailPhotoRef(trailId: String, position: Int) =
    db.document(TrailFields.COLLECTION, trailId, PhotoFields.COLLECTION, position.toString())

class UpdateTrailTransaction(
    private val trailId: String,
    private val editor: ActiveUser,
    private val input: TrailInput,
    private val requestVerification: Boolean,
    private val photos: List<EncodedPhoto>? = null,
) : FirestoreTransaction<Unit>() {

    override fun run(tx: TransactionScope) {
        val problems = TrailValidator.validate(input, pointCount = 2)
        if (problems.isNotEmpty()) throw invalid("The trail is not valid: $problems")
        photos?.let {
            PhotoLimits().requireValidForPost(it)
            if (it.size != input.photoCount) throw invalid("photoCount ${input.photoCount} does not match ${it.size} pictures")
        }

        val trail = tx.readTrail(trailId)                                      // reads first
        val viewer = editor.toViewer()
        if (!TrailRules.canEdit(trail, viewer)) throw denied("Only the author or an admin can edit a trail")
        if (photos == null && input.photoCount != trail.photoCount) throw invalid("Pictures cannot be added or removed without new pictures")

        val replaced = photos != null
        val status = TrailRules.statusAfterEdit(trail, input, viewer, replaced)
        val requested = TrailRules.requestAfterEdit(trail, input, viewer, requestVerification, replaced)

        photos?.forEachIndexed { position, photo -> tx.set(tx.trailPhotoRef(trailId, position), photoFields(photo)) }  // then writes
        for (position in (photos?.size ?: trail.photoCount) until trail.photoCount) tx.delete(tx.trailPhotoRef(trailId, position))

        tx.update(
            tx.trailRef(trailId),
            mapOf(
                TrailFields.NAME to input.name.trim(),
                TrailFields.DESCRIPTION to input.description.trim(),
                TrailFields.LOCATION to input.location.trim(),
                TrailFields.LENGTH_METERS to input.lengthMeters,
                TrailFields.DIFFICULTY to input.difficulty.toLong(),
                TrailFields.VISIBILITY to input.visibility.key,
                TrailFields.PHOTO_COUNT to input.photoCount.toLong(),
                TrailFields.STATUS to status.key,
                TrailFields.VERIFICATION_REQUESTED to requested,
                TrailFields.UPDATED_AT to FieldValue.serverTimestamp(),
            ),
        )
    }
}

/** The author asks for review. Only a public, unverified trail with no pending request qualifies. */
class RequestVerificationTransaction(
    private val trailId: String,
    private val user: ActiveUser,
) : FirestoreTransaction<Unit>() {

    override fun run(tx: TransactionScope) {
        val trail = tx.readTrail(trailId)
        if (trail.ownerId != user.userId) throw denied("Only the author can request verification")
        if (!TrailRules.canRequestVerification(trail, user.toViewer())) {
            throw invalid("Only a public, unverified trail without a pending request can be submitted")
        }
        tx.update(tx.trailRef(trailId), mapOf(TrailFields.VERIFICATION_REQUESTED to true))
    }
}

class SaveTrailReviewTransaction(
    private val trailId: String,
    private val userId: String,
    private val userName: String,
    private val rating: Int,
    private val caption: String,
    private val photos: List<EncodedPhoto>?,
) : FirestoreTransaction<Unit>() {

    override fun run(tx: TransactionScope) {
        if (rating !in 1..5) throw invalid("A rating is 1 to 5 stars but was $rating")
        val body = caption.trim()
        if (body.length > TrailLimits.REVIEW_CAPTION_MAX) throw invalid("A caption holds at most ${TrailLimits.REVIEW_CAPTION_MAX} characters")
        photos?.let {
            if (it.size > TrailLimits.REVIEW_PHOTOS) throw invalid("A review holds at most ${TrailLimits.REVIEW_PHOTOS} pictures")
            PhotoLimits().requireValidForPost(it)
        }

        val reviewRef = tx.db.document(TrailFields.COLLECTION, trailId, TrailReviewFields.COLLECTION, userId)
        tx.readTrail(trailId)                                                   // reads first
        val existing = tx.read(reviewRef, TrailReviewMapper)

        val change = RatingMath.change(existing?.rating, rating)
        if (existing == null) {                                                 // then writes
            val pictures = photos.orEmpty()
            tx.set(
                reviewRef,
                mapOf(
                    TrailReviewFields.USER_ID to userId,
                    TrailReviewFields.USER_NAME to userName,
                    TrailReviewFields.CAPTION to body,
                    TrailReviewFields.RATING to rating.toLong(),
                    TrailReviewFields.PHOTO_COUNT to pictures.size.toLong(),
                    TrailReviewFields.HEART_COUNT to 0L,
                    TrailReviewFields.CREATED_AT to FieldValue.serverTimestamp(),
                    TrailReviewFields.UPDATED_AT to FieldValue.serverTimestamp(),
                ),
            )
            pictures.forEachIndexed { position, photo -> tx.set(tx.reviewPhotoRef(trailId, userId, position), photoFields(photo)) }
        } else {
            tx.update(
                reviewRef,
                mapOf(
                    TrailReviewFields.CAPTION to body,
                    TrailReviewFields.RATING to rating.toLong(),
                    TrailReviewFields.PHOTO_COUNT to (photos?.size ?: existing.photoCount).toLong(),
                    TrailReviewFields.UPDATED_AT to FieldValue.serverTimestamp(),
                ),
            )
            photos?.let { pictures ->
                pictures.forEachIndexed { position, photo -> tx.set(tx.reviewPhotoRef(trailId, userId, position), photoFields(photo)) }
                for (position in pictures.size until existing.photoCount) tx.delete(tx.reviewPhotoRef(trailId, userId, position))
            }
        }

        val counters = buildMap<String, Any?> {
            if (change.sumDelta != 0L) put(TrailFields.RATING_SUM, FieldValue.increment(change.sumDelta))
            if (change.countDelta != 0L) put(TrailFields.RATING_COUNT, FieldValue.increment(change.countDelta))
        }
        if (counters.isNotEmpty()) tx.update(tx.trailRef(trailId), counters)
    }
}

private fun TransactionScope.reviewPhotoRef(trailId: String, reviewId: String, position: Int) =
    db.document(TrailFields.COLLECTION, trailId, TrailReviewFields.COLLECTION, reviewId, PhotoFields.COLLECTION, position.toString())

class DeleteTrailReviewTransaction(
    private val trailId: String,
    private val reviewId: String,
    private val user: ActiveUser,
) : FirestoreTransaction<Unit>() {

    override fun run(tx: TransactionScope) {
        val trail = tx.readTrail(trailId)                                       // reads first
        val reviewRef = tx.db.document(TrailFields.COLLECTION, trailId, TrailReviewFields.COLLECTION, reviewId)
        val review = tx.read(reviewRef, TrailReviewMapper) ?: throw missing("Review $reviewId")

        val isOwnerOrAdmin = trail.ownerId == user.userId || user.isAdmin
        if (review.userId != user.userId && !isOwnerOrAdmin) throw denied("Only the reviewer, the trail's owner or an admin can delete a review")

        for (position in 0 until review.photoCount) tx.delete(tx.reviewPhotoRef(trailId, reviewId, position))   // then writes
        tx.delete(reviewRef)

        val updates = mutableMapOf<String, Any?>(
            TrailFields.RATING_SUM to FieldValue.increment(-review.rating.toLong()),
            TrailFields.RATING_COUNT to FieldValue.increment(-1L),
        )
        if (isOwnerOrAdmin) {
            val kept = trail.galleryPhotos.filterNot { it.startsWith("$reviewId/") }
            if (kept != trail.galleryPhotos) updates[TrailFields.GALLERY_PHOTOS] = kept
        }
        tx.update(tx.trailRef(trailId), updates)
    }
}

/** Gives a review a heart, or takes this user's heart back. Returns true if it is now hearted. */
class HeartTrailReviewTransaction(
    private val trailId: String,
    private val reviewId: String,
    private val userId: String,
) : FirestoreTransaction<Boolean>() {

    override fun run(tx: TransactionScope): Boolean {
        val heartRef = tx.db.document(TrailFields.COLLECTION, trailId, TrailFields.REVIEW_HEARTS, "${reviewId}_$userId")
        val reviewRef = tx.db.document(TrailFields.COLLECTION, trailId, TrailReviewFields.COLLECTION, reviewId)

        tx.read(reviewRef, TrailReviewMapper) ?: throw missing("Review $reviewId")          // reads first
        val already = tx.read(heartRef, PresenceReader) != null

        if (already) {                                                                       // then writes
            tx.delete(heartRef)
        } else {
            tx.set(heartRef, mapOf(TrailReviewHeartFields.REVIEW_ID to reviewId, TrailReviewHeartFields.USER_ID to userId))
        }
        tx.update(reviewRef, mapOf(TrailReviewFields.HEART_COUNT to FieldValue.increment(if (already) -1L else 1L)))
        return !already
    }
}

class PromoteReviewPhotoTransaction(
    private val trailId: String,
    private val reviewId: String,
    private val index: Int,
    private val promote: Boolean,
    private val userId: String,
) : FirestoreTransaction<Unit>() {

    override fun run(tx: TransactionScope) {
        val trail = tx.readTrail(trailId)                                       // reads first
        if (trail.ownerId != userId) throw denied("Only the trail's owner can change the gallery")
        val reviewRef = tx.db.document(TrailFields.COLLECTION, trailId, TrailReviewFields.COLLECTION, reviewId)
        val review = tx.read(reviewRef, TrailReviewMapper) ?: throw missing("Review $reviewId")
        if (index !in 0 until review.photoCount) throw invalid("Review $reviewId has no picture $index")

        val entry = "$reviewId/$index"
        val gallery = when {
            !promote -> trail.galleryPhotos - entry
            entry in trail.galleryPhotos -> trail.galleryPhotos
            trail.galleryPhotos.size >= TrailLimits.PROMOTED_PHOTOS ->
                throw invalid("The gallery holds at most ${TrailLimits.PROMOTED_PHOTOS} promoted pictures")
            else -> trail.galleryPhotos + entry
        }
        tx.update(tx.trailRef(trailId), mapOf(TrailFields.GALLERY_PHOTOS to gallery))   // then writes
    }
}
