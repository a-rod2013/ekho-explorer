package com.comp350sec001group2.ekhoexplorer.Business.Database.Commands

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Database.TransactionScope
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalEntryFields
import com.comp350sec001group2.ekhoexplorer.Business.Journal.JournalDraftEntity
import com.comp350sec001group2.ekhoexplorer.Business.Journal.JournalIdentity
import com.google.firebase.firestore.FieldValue

enum class JournalUploadResult {
    WRITTEN,
    ALREADY_UPLOADED,
    CONFLICT,
}

class UploadJournalDraftTransaction(
    private val draft: JournalDraftEntity,
    private val identity: JournalIdentity,
) : FirestoreTransaction<JournalUploadResult>() {

    override fun run(tx: TransactionScope): JournalUploadResult {
        validateDraft()
        requireCurrentOwner()

        val ref = tx.db.document(
            JournalEntryFields.USERS,
            draft.ownerId,
            JournalEntryFields.COLLECTION,
            draft.id,
        )

        val fields = cloudFields()
        val existing = tx.read(ref)

        if (existing.exists()) {
            val remoteRevision =
                existing.getLong(JournalEntryFields.REVISION)
                    ?: throw FirestoreException(
                        FirestoreErrorKind.SCHEMA_MISMATCH,
                        "Journal document has no valid revision.",
                    )

            val remoteOwner =
                existing.getString(JournalEntryFields.OWNER_ID)

            if (remoteOwner != draft.ownerId) {
                return JournalUploadResult.CONFLICT
            }

            if (remoteRevision > draft.revision) {
                return JournalUploadResult.CONFLICT
            }

            if (remoteRevision == draft.revision) {
                val matches = fields.all { (key, value) ->
                    existing.get(key) == value
                }

                return if (matches) {
                    JournalUploadResult.ALREADY_UPLOADED
                } else {
                    JournalUploadResult.CONFLICT
                }
            }

            if (
                existing.get(
                    JournalEntryFields.DELETED_AT_MILLIS
                ) != null
            ) {
                // A cloud tombstone cannot be edited or resurrected.
                return JournalUploadResult.CONFLICT
            }
        }

        val writeFields = fields.toMutableMap()
        writeFields[JournalEntryFields.SYNCED_AT] =
            FieldValue.serverTimestamp()

        requireCurrentOwner()
        tx.set(ref, writeFields)

        return JournalUploadResult.WRITTEN
    }

    private fun cloudFields(): Map<String, Any?> {
        val deleted = draft.deletedAtMillis != null

        return mapOf(
            JournalEntryFields.ID to draft.id,
            JournalEntryFields.OWNER_ID to draft.ownerId,
            JournalEntryFields.TITLE to
                if (deleted) "" else draft.title,
            JournalEntryFields.NOTES to
                if (deleted) "" else draft.notes,
            JournalEntryFields.CREATED_AT_MILLIS to
                draft.createdAtMillis,
            JournalEntryFields.UPDATED_AT_MILLIS to
                draft.updatedAtMillis,
            JournalEntryFields.REVISION to draft.revision,
            JournalEntryFields.DELETED_AT_MILLIS to
                draft.deletedAtMillis,
        )
    }

    private fun requireCurrentOwner() {
        if (!identity.isCurrentOwner(draft.ownerId)) {
            throw FirestoreException(
                FirestoreErrorKind.PERMISSION_DENIED,
                "Journal owner does not match the signed-in user.",
            )
        }
    }

    private fun validateDraft() {
        val invalidDeletion = draft.deletedAtMillis?.let {
            it < 0L
        } ?: false

        if (
            !validPathSegment(draft.id) ||
            !validPathSegment(draft.ownerId) ||
            draft.revision < 1L ||
            draft.createdAtMillis < 0L ||
            draft.updatedAtMillis < draft.createdAtMillis ||
            invalidDeletion
        ) {
            throw FirestoreException(
                FirestoreErrorKind.INVALID_ARGUMENT,
                "Journal entry has invalid identifiers or revision data.",
            )
        }
    }

    private fun validPathSegment(value: String): Boolean =
        value.isNotBlank() &&
            value != "." &&
            value != ".." &&
            !value.contains("/")
}
