package com.comp350sec001group2.ekhoexplorer.Business.Database.Commands

import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthValidator
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UserProfileFields
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import java.util.Locale

/**
 * Writes users/{uid} for a new account. [userId] must be the Firebase Authentication uid:
 * the security rules only allow a user to create their own profile.
 */
class CreateUserProfileCommand(
    private val userId: String,
    private val username: String,
) : FirestoreCommand<Unit>() {

    override suspend fun run(db: FirestoreContext) {
        val name = username.trim()
        if (AuthValidator.username(name) != null) {
            throw FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, "Invalid username")
        }
        db.document(UserProfileFields.COLLECTION, userId).set(
            mapOf(
                UserProfileFields.USERNAME to name,
                UserProfileFields.USERNAME_LOWER to name.lowercase(Locale.ROOT),
                UserProfileFields.CREATED_AT to FieldValue.serverTimestamp(),
            ),
        ).await()
    }
}
