package com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards

import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.CardPhoto
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalCard
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalNote
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalObservation
import com.comp350sec001group2.ekhoexplorer.Business.Database.StreamUpdate
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

data class NewObservation(
    val card: CardIdentity,
    val observedAt: Long,
    val isPublic: Boolean,
    val location: GeoPoint?,
    val photoSources: List<() -> InputStream>,
)

interface JournalPhotoLoader {
    suspend fun cardPhotos(ownerId: String, cardId: String): List<CardPhoto>

    suspend fun observationPhoto(observationId: String, index: Int): ByteArray?
}

interface JournalCardRepository : JournalPhotoLoader {
    fun observeCards(): Flow<StreamUpdate<List<JournalCard>>>
    fun observeCard(cardId: String): Flow<StreamUpdate<JournalCard?>>
    fun observeObservations(cardId: String): Flow<StreamUpdate<List<JournalObservation>>>
    fun observeNotes(cardId: String): Flow<StreamUpdate<List<JournalNote>>>

    suspend fun visitorCards(ownerId: String): List<JournalCard>

    suspend fun record(newObservation: NewObservation): String

    suspend fun deleteObservation(observationId: String): Boolean

    suspend fun identify(observationId: String, target: CardIdentity)

    suspend fun saveNote(cardId: String, noteId: String?, text: String): String
    suspend fun deleteNote(cardId: String, noteId: String)
    suspend fun setPinned(cardId: String, pinned: Boolean)
    suspend fun setJournalPrivate(isPrivate: Boolean)

    suspend fun clearGlobalPins(): Int

    suspend fun clear()
}
