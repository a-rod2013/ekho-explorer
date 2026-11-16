package com.comp350sec001group2.ekhoexplorer.Business.Database.Batches

import com.comp350sec001group2.ekhoexplorer.Business.Database.BatchScope
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreBatch
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.ObservationFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PhotoFields
import com.comp350sec001group2.ekhoexplorer.Business.Photos.EncodedPhoto
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FieldValue

class AddObservationWithPhotosBatch(
    private val observationId: String,
    private val userId: String,
    private val commonName: String,
    private val scientificName: String,
    private val category: String,
    private val description: String,
    private val visibility: String,
    private val latitude: Double,
    private val longitude: Double,
    private val photos: List<EncodedPhoto>,
) : FirestoreBatch() {

    override fun stage(batch: BatchScope) {
        if (photos.isEmpty()) {
            throw IllegalArgumentException("An observation must have a photo")
        }

        val observation = mapOf(
            ObservationFields.USER_ID to userId,
            ObservationFields.COMMON_NAME to commonName,
            ObservationFields.SCIENTIFIC_NAME to scientificName,
            ObservationFields.CATEGORY to category,
            ObservationFields.DESCRIPTION to description,
            ObservationFields.LATITUDE to latitude,
            ObservationFields.LONGITUDE to longitude,
            ObservationFields.PHOTO_COUNT to photos.size.toLong(),
            ObservationFields.VISIBILITY to visibility,
            ObservationFields.CREATED_AT to FieldValue.serverTimestamp(),
        )

        batch.set(
            batch.db.document(ObservationFields.COLLECTION, observationId),
            observation
        )

        photos.forEachIndexed { position, photo ->
            batch.set(
                batch.db.document(
                    ObservationFields.COLLECTION,
                    observationId,
                    PhotoFields.COLLECTION,
                    position.toString()
                ),
                mapOf(
                    PhotoFields.DATA to Blob.fromBytes(photo.bytes),
                    PhotoFields.WIDTH to photo.width.toLong(),
                    PhotoFields.HEIGHT to photo.height.toLong(),
                )
            )
        }
    }
}
