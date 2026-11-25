package com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories

import android.util.Log
import com.comp350sec001group2.ekhoexplorer.Business.Cache.ByteCache
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Cache.NoByteCache
import com.comp350sec001group2.ekhoexplorer.Business.Cache.cached
import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.ClearGlobalPinsCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.DeleteNoteCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.PurgeCardNotesCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.SaveNoteCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.SetCardPinnedCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.SetJournalPrivateCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreIds
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreService
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.CardPhoto
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalCard
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalNote
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalObservation
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetCardPhotosQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetJournalPhotoQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetObservationEndsQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetObservationQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetVisitorCardsQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.StreamUpdate
import com.comp350sec001group2.ekhoexplorer.Business.Database.Streams.ObserveCardStream
import com.comp350sec001group2.ekhoexplorer.Business.Database.Streams.ObserveCardsStream
import com.comp350sec001group2.ekhoexplorer.Business.Database.Streams.ObserveNotesStream
import com.comp350sec001group2.ekhoexplorer.Business.Database.Streams.ObserveObservationsStream
import com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions.DeleteObservationTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions.IdentifyObservationTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions.RecordObservationTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards.CardIdentity
import com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards.JournalCardRepository
import com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards.NewObservation
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoPreparer
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap

class FirestoreJournalCardRepository(
    private val service: FirestoreService,
    private val photoPreparer: PhotoPreparer,
    private val thumbnailPreparer: PhotoPreparer,
    private val byteCache: ByteCache = NoByteCache,
    private val ownerIdProvider: () -> String = { EhkoCache.activeUser.userId },
) : JournalCardRepository {

    private val cardPhotoMemo = ConcurrentHashMap<String, List<CardPhoto>>()

    private fun owner(): String = ownerIdProvider().takeIf { it.isNotBlank() }
        ?: throw FirestoreException(FirestoreErrorKind.PERMISSION_DENIED, "Sign in to use the journal")

    override fun observeCards(): Flow<StreamUpdate<List<JournalCard>>> = service.stream(ObserveCardsStream(owner()))
    override fun observeCard(cardId: String): Flow<StreamUpdate<JournalCard?>> = service.stream(ObserveCardStream(owner(), cardId))
    override fun observeObservations(cardId: String): Flow<StreamUpdate<List<JournalObservation>>> =
        service.stream(ObserveObservationsStream(owner(), cardId))
    override fun observeNotes(cardId: String): Flow<StreamUpdate<List<JournalNote>>> = service.stream(ObserveNotesStream(owner(), cardId))

    override suspend fun visitorCards(ownerId: String): List<JournalCard> = service.execute(GetVisitorCardsQuery(ownerId))

    override suspend fun record(newObservation: NewObservation): String {
        val ownerId = owner()
        val sources = newObservation.photoSources
        if (sources.isEmpty() || sources.size > MAX_OBSERVATION_PHOTOS) {
            throw FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, "An observation needs 1 to $MAX_OBSERVATION_PHOTOS photos")
        }
        val photos = photoPreparer.prepare(sources)
        val thumbnail = thumbnailPreparer.prepare(listOf(sources.first())).single()
        val cardId = service.execute(
            RecordObservationTransaction(
                ownerId = ownerId,
                observationId = FirestoreIds.newId(),
                card = newObservation.card,
                observedAt = newObservation.observedAt,
                isPublic = newObservation.isPublic,
                location = newObservation.location,
                photos = photos,
                thumbnail = thumbnail,
            ),
        )
        cardPhotoMemo.remove(cardKey(ownerId, cardId))
        return cardId
    }

    override suspend fun deleteObservation(observationId: String): Boolean {
        val ownerId = owner()
        val observation = service.execute(GetObservationQuery(ownerId, observationId))
            ?: throw FirestoreException(FirestoreErrorKind.NOT_FOUND, "Observation $observationId does not exist")
        val ends = service.execute(GetObservationEndsQuery(ownerId, observation.cardId, observationId))
        val cardDeleted = service.execute(DeleteObservationTransaction(ownerId, observationId, ends.first, ends.last))
        byteCache.remove("journal", ownerId, "obs_$observationId")
        cardPhotoMemo.remove(cardKey(ownerId, observation.cardId))
        if (cardDeleted) {
            // The card is gone; clearing its notes is tidying, so a failure here does not undo the delete.
            try {
                service.execute(PurgeCardNotesCommand(ownerId, observation.cardId))
            } catch (e: FirestoreException) {
                Log.w("JournalRepository", "Could not clear the notes of card ${observation.cardId}: ${e.message}")
            }
        }
        return cardDeleted
    }

    override suspend fun identify(observationId: String, target: CardIdentity) {
        val ownerId = owner()
        val observation = service.execute(GetObservationQuery(ownerId, observationId))
            ?: throw FirestoreException(FirestoreErrorKind.NOT_FOUND, "Observation $observationId does not exist")
        val ends = service.execute(GetObservationEndsQuery(ownerId, observation.cardId, observationId))
        service.execute(IdentifyObservationTransaction(ownerId, observationId, target, ends.first, ends.last))
        cardPhotoMemo.remove(cardKey(ownerId, observation.cardId))
        cardPhotoMemo.remove(cardKey(ownerId, target.cardId))
    }

    override suspend fun saveNote(cardId: String, noteId: String?, text: String): String =
        service.execute(SaveNoteCommand(owner(), cardId, noteId, text))

    override suspend fun deleteNote(cardId: String, noteId: String) {
        service.execute(DeleteNoteCommand(owner(), cardId, noteId))
    }

    override suspend fun setPinned(cardId: String, pinned: Boolean) {
        service.execute(SetCardPinnedCommand(owner(), cardId, pinned))
    }

    override suspend fun setJournalPrivate(isPrivate: Boolean) {
        service.execute(SetJournalPrivateCommand(owner(), isPrivate))
    }

    override suspend fun clearGlobalPins(): Int = service.execute(ClearGlobalPinsCommand(owner()))

    override suspend fun cardPhotos(ownerId: String, cardId: String): List<CardPhoto> {
        val key = cardKey(ownerId, cardId)
        cardPhotoMemo[key]?.let { return it }
        return service.execute(GetCardPhotosQuery(ownerId, cardId)).also { cardPhotoMemo[key] = it }
    }

    override suspend fun observationPhoto(observationId: String, index: Int): ByteArray? {
        val ownerId = owner()
        return byteCache.cached("journal", ownerId, "obs_$observationId", index.toString()) {
            service.execute(GetJournalPhotoQuery(ownerId, observationId, index))?.bytes
        }
    }

    override suspend fun clear() {
        cardPhotoMemo.clear()
    }

    private fun cardKey(ownerId: String, cardId: String) = "$ownerId/$cardId"

    private companion object {
        const val MAX_OBSERVATION_PHOTOS = 3
    }
}
