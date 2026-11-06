package com.comp350sec001group2.ekhoexplorer.Business.Database.Commands

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UserProfileFields
import kotlinx.coroutines.tasks.await

/***
 * Updates the bio field of an existing user profile
 * The userId must belong to the currently authenticated user
 */
class UpdateUserBioCommand(
    private val userId: String,
    private val bio: String,
) : FirestoreCommand<Unit>() {

    override suspend fun run(db: FirestoreContext) {

        if (userId.isBlank() || bio.length > 160) {
            throw FirestoreException(
                FirestoreErrorKind.INVALID_ARGUMENT,
                "Invalid user ID or bio exceeds 160 characters"
            )
        }

        db.document(UserProfileFields.COLLECTION, userId)
            .update(UserProfileFields.BIO, bio)
            .await()
    }
}
