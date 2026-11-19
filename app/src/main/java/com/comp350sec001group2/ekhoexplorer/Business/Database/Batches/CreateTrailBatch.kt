package com.comp350sec001group2.ekhoexplorer.Business.Database.Batches

import com.comp350sec001group2.ekhoexplorer.Business.Cache.ActiveUser
import com.comp350sec001group2.ekhoexplorer.Business.Database.BatchScope
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreBatch
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PhotoFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailFields
import com.comp350sec001group2.ekhoexplorer.Business.Photos.EncodedPhoto
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoLimits
import com.comp350sec001group2.ekhoexplorer.Business.Photos.requireValidForPost
import com.comp350sec001group2.ekhoexplorer.Business.Trails.LatLngPoint
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailGeo
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailInput
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailStatus
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailValidator
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailVisibility
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FieldValue

class CreateTrailBatch(
    val trailId: String,
    private val owner: ActiveUser,
    private val input: TrailInput,
    private val path: List<LatLngPoint>,
    private val photos: List<EncodedPhoto>,
    private val requestVerification: Boolean,
) : FirestoreBatch() {

    override fun stage(batch: BatchScope) {
        val problems = TrailValidator.validate(input, path.size)
        if (problems.isNotEmpty()) {
            throw FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, "The trail is not valid: $problems")
        }
        PhotoLimits().requireValidForPost(photos)
        if (photos.size != input.photoCount) {
            throw FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, "photoCount ${input.photoCount} does not match ${photos.size} pictures")
        }

        val start = path.first()
        val fields = mapOf<String, Any?>(
            TrailFields.OWNER_ID to owner.userId,
            TrailFields.OWNER_NAME to owner.userName,
            TrailFields.NAME to input.name.trim(),
            TrailFields.DESCRIPTION to input.description.trim(),
            TrailFields.LOCATION to input.location.trim(),
            TrailFields.LENGTH_METERS to input.lengthMeters,
            TrailFields.DIFFICULTY to input.difficulty.toLong(),
            TrailFields.VISIBILITY to input.visibility.key,
            TrailFields.STATUS to TrailStatus.UNVERIFIED.key,
            TrailFields.VERIFICATION_REQUESTED to (requestVerification && input.visibility == TrailVisibility.PUBLIC),
            TrailFields.PATH to TrailGeo.encode(path),
            TrailFields.START_LAT to start.lat,
            TrailFields.START_LNG to start.lng,
            TrailFields.PHOTO_COUNT to photos.size.toLong(),
            TrailFields.GALLERY_PHOTOS to emptyList<String>(),
            TrailFields.RATING_SUM to 0L,
            TrailFields.RATING_COUNT to 0L,
            TrailFields.CREATED_AT to FieldValue.serverTimestamp(),
            TrailFields.UPDATED_AT to FieldValue.serverTimestamp(),
        )
        batch.set(batch.db.document(TrailFields.COLLECTION, trailId), fields)

        photos.forEachIndexed { position, photo ->
            batch.set(
                batch.db.document(TrailFields.COLLECTION, trailId, PhotoFields.COLLECTION, position.toString()),
                photoFields(photo),
            )
        }
    }
}

internal fun photoFields(photo: EncodedPhoto): Map<String, Any?> = mapOf(
    PhotoFields.DATA to Blob.fromBytes(photo.bytes),
    PhotoFields.WIDTH to photo.width.toLong(),
    PhotoFields.HEIGHT to photo.height.toLong(),
)
