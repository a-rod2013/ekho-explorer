package com.comp350sec001group2.ekhoexplorer.Business.Database.Commands

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreIds
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalCardFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalObservationFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.NoteFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PhotoFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PinFields
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

object NoteText {
    const val MAX = 500

    fun validate(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) throw FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, "A note cannot be empty")
        if (trimmed.length > MAX) throw FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, "A note holds at most $MAX characters")
        return trimmed
    }
}

class SaveNoteCommand(
    private val ownerId: String,
    private val cardId: String,
    private val noteId: String?,
    private val text: String,
) : FirestoreCommand<String>() {
    override suspend fun run(db: FirestoreContext): String {
        val body = NoteText.validate(text)
        val id = noteId ?: FirestoreIds.newId()
        val ref = db.document(JournalCardFields.USERS, ownerId, JournalCardFields.COLLECTION, cardId, JournalCardFields.NOTES, id)
        if (noteId == null) {
            ref.set(
                mapOf(
                    NoteFields.TEXT to body,
                    NoteFields.CREATED_AT to FieldValue.serverTimestamp(),
                    NoteFields.UPDATED_AT to FieldValue.serverTimestamp(),
                ),
            ).await()
        } else {
            ref.update(mapOf(NoteFields.TEXT to body, NoteFields.UPDATED_AT to FieldValue.serverTimestamp())).await()
        }
        return id
    }
}

class DeleteNoteCommand(private val ownerId: String, private val cardId: String, private val noteId: String) : FirestoreCommand<Unit>() {
    override suspend fun run(db: FirestoreContext) {
        db.document(JournalCardFields.USERS, ownerId, JournalCardFields.COLLECTION, cardId, JournalCardFields.NOTES, noteId).delete().await()
    }
}

class SetCardPinnedCommand(private val ownerId: String, private val cardId: String, private val pinned: Boolean) : FirestoreCommand<Unit>() {
    override suspend fun run(db: FirestoreContext) {
        db.document(JournalCardFields.USERS, ownerId, JournalCardFields.COLLECTION, cardId)
            .update(mapOf(JournalCardFields.PINNED to pinned, JournalCardFields.UPDATED_AT to FieldValue.serverTimestamp()))
            .await()
    }
}

class SetJournalPrivateCommand(private val ownerId: String, private val isPrivate: Boolean) : FirestoreCommand<Unit>() {
    override suspend fun run(db: FirestoreContext) {
        db.document(JournalCardFields.USERS, ownerId).update(mapOf(JournalCardFields.JOURNAL_PRIVATE to isPrivate)).await()
    }
}

class ClearGlobalPinsCommand(private val ownerId: String) : FirestoreCommand<Int>() {
    override suspend fun run(db: FirestoreContext): Int {
        var cleared = 0
        while (true) {
            val pins = db.collection(PinFields.COLLECTION)
                .whereEqualTo(PinFields.OWNER_ID, ownerId)
                .whereEqualTo(PinFields.VISIBILITY, JournalObservationFields.PUBLIC)
                .limit(CHUNK)
                .get().await().documents
            if (pins.isEmpty()) return cleared
            val batch = db.firestore.batch()
            pins.forEach { pin ->
                batch.delete(pin.reference.collection(PinFields.PHOTO).document("0"))
                batch.delete(pin.reference)
            }
            batch.commit().await()
            cleared += pins.size
            if (pins.size < CHUNK) return cleared
        }
    }

    private companion object { const val CHUNK = 200L }
}

class PurgeCardNotesCommand(private val ownerId: String, private val cardId: String) : FirestoreCommand<Unit>() {
    override suspend fun run(db: FirestoreContext) {
        val notes = db.collection(JournalCardFields.USERS, ownerId, JournalCardFields.COLLECTION, cardId, JournalCardFields.NOTES)
        while (true) {
            val docs = notes.limit(CHUNK).get().await().documents
            if (docs.isEmpty()) return
            val batch = db.firestore.batch()
            docs.forEach { batch.delete(it.reference) }
            batch.commit().await()
            if (docs.size < CHUNK) return
        }
    }

    private companion object { const val CHUNK = 400L }
}
