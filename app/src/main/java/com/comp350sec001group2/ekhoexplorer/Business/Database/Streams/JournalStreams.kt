package com.comp350sec001group2.ekhoexplorer.Business.Database.Streams

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreStream
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalCard
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalCardFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalCardMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalNote
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalNoteMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalObservation
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalObservationFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalObservationMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.NoteFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.StreamUpdate
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow

class ObserveCardsStream(private val ownerId: String) : FirestoreStream<StreamUpdate<List<JournalCard>>>() {
    override fun observe(db: FirestoreContext): Flow<StreamUpdate<List<JournalCard>>> = db.observeQuery(
        query = db.collection(JournalCardFields.USERS, ownerId, JournalCardFields.COLLECTION)
            .orderBy(JournalCardFields.LAST_OBSERVED_AT, Query.Direction.DESCENDING),
        limit = 500,
        reader = JournalCardMapper,
        includeMetadataChanges = true,
    )
}

class ObserveCardStream(private val ownerId: String, private val cardId: String) : FirestoreStream<StreamUpdate<JournalCard?>>() {
    override fun observe(db: FirestoreContext): Flow<StreamUpdate<JournalCard?>> = db.observeDocument(
        ref = db.document(JournalCardFields.USERS, ownerId, JournalCardFields.COLLECTION, cardId),
        reader = JournalCardMapper,
        includeMetadataChanges = true,
    )
}

class ObserveObservationsStream(private val ownerId: String, private val cardId: String) : FirestoreStream<StreamUpdate<List<JournalObservation>>>() {
    override fun observe(db: FirestoreContext): Flow<StreamUpdate<List<JournalObservation>>> = db.observeQuery(
        query = db.collection(JournalCardFields.USERS, ownerId, JournalObservationFields.COLLECTION)
            .whereEqualTo(JournalObservationFields.CARD_ID, cardId)
            .orderBy(JournalObservationFields.OBSERVED_AT, Query.Direction.DESCENDING),
        limit = 200,
        reader = JournalObservationMapper,
        includeMetadataChanges = true,
    )
}

class ObserveNotesStream(private val ownerId: String, private val cardId: String) : FirestoreStream<StreamUpdate<List<JournalNote>>>() {
    override fun observe(db: FirestoreContext): Flow<StreamUpdate<List<JournalNote>>> = db.observeQuery(
        query = db.collection(JournalCardFields.USERS, ownerId, JournalCardFields.COLLECTION, cardId, JournalCardFields.NOTES)
            .orderBy(NoteFields.CREATED_AT, Query.Direction.DESCENDING),
        limit = 200,
        reader = JournalNoteMapper,
        includeMetadataChanges = true,
    )
}
