package com.comp350sec001group2.ekhoexplorer.Business.Database.Queries

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.CardPhoto
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.CardPhotoMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalCard
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalCardFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalCardMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalObservation
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalObservationFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalObservationMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PhotoFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostPhotoMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostPhoto
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

data class ObservationEnds(val first: Long?, val last: Long?) {
    companion object {
        fun from(oldest: List<JournalObservation>, newest: List<JournalObservation>, excludeId: String) = ObservationEnds(
            first = oldest.firstOrNull { it.id != excludeId }?.observedAt,
            last = newest.firstOrNull { it.id != excludeId }?.observedAt,
        )
    }
}

class GetObservationEndsQuery(
    private val ownerId: String,
    private val cardId: String,
    private val excludeObservationId: String,
) : FirestoreQuery<ObservationEnds>() {
    override suspend fun run(db: FirestoreContext): ObservationEnds {
        val base = db.collection(JournalCardFields.USERS, ownerId, JournalObservationFields.COLLECTION)
            .whereEqualTo(JournalObservationFields.CARD_ID, cardId)
        val oldest = db.getDocuments(base.orderBy(JournalObservationFields.OBSERVED_AT, Query.Direction.ASCENDING), 2, JournalObservationMapper)
        val newest = db.getDocuments(base.orderBy(JournalObservationFields.OBSERVED_AT, Query.Direction.DESCENDING), 2, JournalObservationMapper)
        return ObservationEnds.from(oldest, newest, excludeObservationId)
    }
}

class GetObservationQuery(private val ownerId: String, private val observationId: String) : FirestoreQuery<JournalObservation?>() {
    override suspend fun run(db: FirestoreContext): JournalObservation? =
        db.getDocument(db.document(JournalCardFields.USERS, ownerId, JournalObservationFields.COLLECTION, observationId), JournalObservationMapper)
}

class GetVisitorCardsQuery(private val ownerId: String, private val limit: Int = 200) : FirestoreQuery<List<JournalCard>>() {
    override suspend fun run(db: FirestoreContext): List<JournalCard> =
        db.getDocuments(
            db.collection(JournalCardFields.USERS, ownerId, JournalCardFields.COLLECTION)
                .orderBy(JournalCardFields.LAST_OBSERVED_AT, Query.Direction.DESCENDING),
            limit,
            JournalCardMapper,
        )
}

class GetCardPhotosQuery(private val ownerId: String, private val cardId: String) : FirestoreQuery<List<CardPhoto>>() {
    override suspend fun run(db: FirestoreContext): List<CardPhoto> =
        db.getDocuments(
            db.collection(JournalCardFields.USERS, ownerId, JournalCardFields.COLLECTION, cardId, JournalCardFields.CARD_PHOTOS),
            5,
            CardPhotoMapper,
        ).sortedBy { it.addedAtMillis }
}

class GetJournalPhotoQuery(
    private val ownerId: String,
    private val observationId: String,
    private val index: Int,
) : FirestoreQuery<PostPhoto?>() {
    override suspend fun run(db: FirestoreContext): PostPhoto? = db.getDocument(
        db.document(JournalCardFields.USERS, ownerId, JournalObservationFields.COLLECTION, observationId, PhotoFields.COLLECTION, index.toString()),
        PostPhotoMapper,
    )
}

class GetVerifiedCardCountQuery(private val ownerId: String) : FirestoreQuery<Int>() {
    override suspend fun run(db: FirestoreContext): Int =
        db.collection(JournalCardFields.USERS, ownerId, JournalCardFields.COLLECTION)
            .whereEqualTo(JournalCardFields.VERIFIED, true)
            .count()
            .get(AggregateSource.SERVER)
            .await()
            .count
            .toInt()
}
