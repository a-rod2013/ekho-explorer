package com.comp350sec001group2.ekhoexplorer.Business.Database.Commands

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UserAvatarFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UserProfileFields
import com.comp350sec001group2.ekhoexplorer.Business.Photos.EncodedPhoto
import com.google.firebase.firestore.Blob
import kotlinx.coroutines.tasks.await

/**
 * Creates or replaces the current avatar for a user at:
 * users/{userId}/avatar/current
 */
class UpdateUserAvatarCommand(
    private val userId: String,
    private val photo: EncodedPhoto,
) : FirestoreCommand<Unit>() {

    override suspend fun run(db: FirestoreContext) {

        if (userId.isBlank() ||
            photo.bytes.isEmpty() ||
            photo.sizeBytes > 100_000 ||
            photo.width <= 0 ||
            photo.height <= 0
        ) {
            throw FirestoreException(
                FirestoreErrorKind.INVALID_ARGUMENT,
                "Invalid user ID or avatar image"
            )
        }

        db.document(
            UserProfileFields.COLLECTION,
            userId,
            UserAvatarFields.COLLECTION,
            UserAvatarFields.DOCUMENT
        ).set(
            mapOf(
                UserAvatarFields.DATA to Blob.fromBytes(photo.bytes),
                UserAvatarFields.WIDTH to photo.width,
                UserAvatarFields.HEIGHT to photo.height
            )
        ).await()
    }
}
