package com.comp350sec001group2.ekhoexplorer.Business.Database.Commands

import com.comp350sec001group2.ekhoexplorer.Business.Auth.AuthValidator
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.SupportRequestFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UsernameChangeRequest
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UsernameChangeRequestMapper
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class GetUsernameChangeRequestQuery(private val userId: String) : FirestoreQuery<UsernameChangeRequest?>() {
    override suspend fun run(db: FirestoreContext): UsernameChangeRequest? =
        db.getDocument(
            db.document(SupportRequestFields.COLLECTION, SupportRequestFields.usernameChangeId(userId)),
            UsernameChangeRequestMapper,
        )
}

class RequestUsernameChangeCommand(
    private val userId: String,
    private val currentUsername: String,
    private val requestedUsername: String,
    private val reason: String,
) : FirestoreCommand<Unit>() {
    override suspend fun run(db: FirestoreContext) {
        if (AuthValidator.requestedUsername(requestedUsername, currentUsername) != null || AuthValidator.reason(reason) != null) {
            throw FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, "Invalid username change request")
        }
        db.document(SupportRequestFields.COLLECTION, SupportRequestFields.usernameChangeId(userId)).set(
            mapOf(
                SupportRequestFields.USER_ID to userId,
                SupportRequestFields.TYPE to SupportRequestFields.TYPE_USERNAME_CHANGE,
                SupportRequestFields.CURRENT_USERNAME to currentUsername,
                SupportRequestFields.REQUESTED_USERNAME to requestedUsername.trim(),
                SupportRequestFields.REASON to reason.trim(),
                SupportRequestFields.STATUS to "pending",
                SupportRequestFields.CREATED_AT to FieldValue.serverTimestamp(),
            ),
        ).await()
    }
}
