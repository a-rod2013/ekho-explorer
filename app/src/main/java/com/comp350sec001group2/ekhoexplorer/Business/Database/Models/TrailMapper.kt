package com.comp350sec001group2.ekhoexplorer.Business.Database.Models

import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentData
import android.util.Log
import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentReader
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailAccess
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailLimits
import com.comp350sec001group2.ekhoexplorer.Business.Trails.Trail
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailStatus
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailVisibility

object TrailMapper : DocumentReader<Trail> {
    override fun read(doc: DocumentData): Trail {
        val visibilityKey = doc.string(TrailFields.VISIBILITY)
        val statusKey = doc.string(TrailFields.STATUS)
        val difficulty = doc.int(TrailFields.DIFFICULTY)
        if (difficulty !in TrailLimits.DIFFICULTY_MIN..TrailLimits.DIFFICULTY_MAX) {
            throw doc.mismatch("field 'difficulty' must be ${TrailLimits.DIFFICULTY_MIN} to ${TrailLimits.DIFFICULTY_MAX} but was $difficulty")
        }
        return Trail(
            id = doc.id,
            ownerId = doc.string(TrailFields.OWNER_ID),
            ownerName = doc.stringOrNull(TrailFields.OWNER_NAME) ?: "",
            name = doc.string(TrailFields.NAME),
            description = doc.stringOrNull(TrailFields.DESCRIPTION) ?: "",
            location = doc.stringOrNull(TrailFields.LOCATION) ?: "",
            lengthMeters = doc.double(TrailFields.LENGTH_METERS),
            difficulty = difficulty,
            visibility = TrailVisibility.fromKey(visibilityKey) ?: throw doc.mismatch("unknown visibility '$visibilityKey'"),
            status = TrailStatus.fromKey(statusKey) ?: throw doc.mismatch("unknown status '$statusKey'"),
            verificationRequested = doc.booleanOrNull(TrailFields.VERIFICATION_REQUESTED) ?: false,
            pathEncoded = doc.stringOrNull(TrailFields.PATH) ?: "",
            startLat = doc.doubleOrNull(TrailFields.START_LAT),
            startLng = doc.doubleOrNull(TrailFields.START_LNG),
            photoCount = doc.longOrNull(TrailFields.PHOTO_COUNT)?.toInt() ?: 0,
            galleryPhotos = doc.stringList(TrailFields.GALLERY_PHOTOS),
            ratingSum = doc.longOrNull(TrailFields.RATING_SUM) ?: 0L,
            ratingCount = doc.longOrNull(TrailFields.RATING_COUNT) ?: 0L,
            // A just-written document can briefly have no server time yet.
            createdAtMillis = doc.instantOrNull(TrailFields.CREATED_AT)?.toEpochMilli() ?: 0L,
            updatedAtMillis = doc.instantOrNull(TrailFields.UPDATED_AT)?.toEpochMilli() ?: 0L,
        )
    }
}

object LenientTrailMapper : DocumentReader<Trail?> {
    override fun read(doc: DocumentData): Trail? = try {
        TrailMapper.read(doc)
    } catch (e: FirestoreException) {
        Log.w("TrailMapper", "Skipping trail ${doc.id}: ${e.message}")
        null
    }
}

object TrailReviewMapper : DocumentReader<TrailReview> {
    override fun read(doc: DocumentData): TrailReview {
        val rating = doc.int(TrailReviewFields.RATING)
        if (rating !in 1..5) throw doc.mismatch("field 'rating' must be 1 to 5 but was $rating")
        return TrailReview(
            id = doc.id,
            // trails/{trailId}/reviews/{reviewId}
            trailId = doc.path.split('/').getOrNull(1) ?: throw doc.mismatch("review path '${doc.path}' has no trail"),
            userId = doc.string(TrailReviewFields.USER_ID),
            userName = doc.stringOrNull(TrailReviewFields.USER_NAME) ?: "",
            caption = doc.stringOrNull(TrailReviewFields.CAPTION) ?: "",
            rating = rating,
            photoCount = doc.longOrNull(TrailReviewFields.PHOTO_COUNT)?.toInt() ?: 0,
            heartCount = doc.longOrNull(TrailReviewFields.HEART_COUNT)?.toInt() ?: 0,
            createdAtMillis = doc.instantOrNull(TrailReviewFields.CREATED_AT)?.toEpochMilli() ?: 0L,
            updatedAtMillis = doc.instantOrNull(TrailReviewFields.UPDATED_AT)?.toEpochMilli() ?: 0L,
        )
    }
}

object TrailReviewHeartMapper : DocumentReader<String> {
    override fun read(doc: DocumentData): String = doc.string(TrailReviewHeartFields.REVIEW_ID)
}

object TrailAccessMapper : DocumentReader<TrailAccess> {
    override fun read(doc: DocumentData) = TrailAccess(
        isAdmin = doc.stringOrNull(UserTrailFields.PERMISSION) == UserTrailFields.ADMIN,
        following = doc.stringList(UserTrailFields.FOLLOWING).toSet(),
    )
}

object PresenceReader : DocumentReader<Boolean> {
    override fun read(doc: DocumentData) = true
}

