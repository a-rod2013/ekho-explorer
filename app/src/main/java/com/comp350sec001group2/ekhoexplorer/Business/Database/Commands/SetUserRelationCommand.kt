package com.comp350sec001group2.ekhoexplorer.Business.Database.Commands

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.RelationType
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UserRelationFields
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

/** Records that [ownerId] blocked / hid [targetId]. */
class SetUserRelationCommand(
    private val ownerId: String,
    private val targetId: String,
    private val type: RelationType,
) : FirestoreCommand<Unit>() {

    override suspend fun run(db: FirestoreContext) {
        if (ownerId.isEmpty() || targetId.isEmpty() || ownerId == targetId) {
            throw FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, "Need two different users")
        }
        db.document(UserRelationFields.COLLECTION, UserRelationFields.documentId(ownerId, targetId, type))
            .set(
                mapOf(
                    UserRelationFields.OWNER_ID to ownerId,
                    UserRelationFields.TARGET_ID to targetId,
                    UserRelationFields.TYPE to type.name,
                    UserRelationFields.CREATED_AT to FieldValue.serverTimestamp(),
                ),
            ).await()
    }
}
