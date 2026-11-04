package com.comp350sec001group2.ekhoexplorer.Business.Database.Batches

import com.comp350sec001group2.ekhoexplorer.Business.Database.BatchScope
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreBatch
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PhotoFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostFields
import com.comp350sec001group2.ekhoexplorer.Business.Photos.EncodedPhoto
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoLimits
import com.comp350sec001group2.ekhoexplorer.Business.Photos.requireValidForPost
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FieldValue

class AddFeedPostWithPhotosBatch(
    val postId: String,
    private val userId: String,
    private val username: String,
    private val avatarUrl: String?,
    private val caption: String,
    private val photos: List<EncodedPhoto>,
    private val limits: PhotoLimits = PhotoLimits(),
) : FirestoreBatch() {

    override fun stage(batch: BatchScope) {
        if (photos.isEmpty()) {
            throw FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, "Use AddFeedPostCommand for a post without photos")
        }
        limits.requireValidForPost(photos)

        val post = mutableMapOf<String, Any?>(
            PostFields.USER_ID to userId,
            PostFields.USERNAME to username,
            PostFields.IMAGE_URLS to emptyList<String>(),
            PostFields.CAPTION to caption.trim(),
            PostFields.UP_COUNT to 0L,
            PostFields.DOWN_COUNT to 0L,
            PostFields.PHOTO_COUNT to photos.size.toLong(),
            PostFields.CREATED_AT to FieldValue.serverTimestamp(),
        )
        avatarUrl?.let { post[PostFields.AVATAR_URL] = it }
        batch.set(batch.db.document(PostFields.COLLECTION, postId), post)

        photos.forEachIndexed { position, photo ->
            batch.set(
                batch.db.document(PostFields.COLLECTION, postId, PhotoFields.COLLECTION, position.toString()),
                mapOf(
                    PhotoFields.DATA to Blob.fromBytes(photo.bytes),
                    PhotoFields.WIDTH to photo.width.toLong(),
                    PhotoFields.HEIGHT to photo.height.toLong(),
                ),
            )
        }
    }
}
