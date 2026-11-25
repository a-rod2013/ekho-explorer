package com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.CardPhoto
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.CardPhotoFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.CardPhotoMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalCard
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalCardFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalCardMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalObservationFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PhotoFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PinFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.TransactionScope
import com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards.CardIdentity
import com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards.CardMath
import com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards.CardSummary
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import java.util.Date

internal const val CARD_PHOTO_SLOTS = 5
internal const val MAX_STORED_BYTES = 100_000

internal fun invalidJournal(detail: String) = FirestoreException(FirestoreErrorKind.INVALID_ARGUMENT, detail)
internal fun missingJournal(what: String) = FirestoreException(FirestoreErrorKind.NOT_FOUND, "$what does not exist")

internal fun TransactionScope.cardRef(ownerId: String, cardId: String): DocumentReference =
    db.document(JournalCardFields.USERS, ownerId, JournalCardFields.COLLECTION, cardId)

internal fun TransactionScope.cardPhotoRef(ownerId: String, cardId: String, slot: Int): DocumentReference =
    db.document(JournalCardFields.USERS, ownerId, JournalCardFields.COLLECTION, cardId, JournalCardFields.CARD_PHOTOS, slot.toString())

internal fun TransactionScope.observationRef(ownerId: String, observationId: String): DocumentReference =
    db.document(JournalCardFields.USERS, ownerId, JournalObservationFields.COLLECTION, observationId)

internal fun TransactionScope.observationPhotoRef(ownerId: String, observationId: String, index: Int): DocumentReference =
    db.document(JournalCardFields.USERS, ownerId, JournalObservationFields.COLLECTION, observationId, PhotoFields.COLLECTION, index.toString())

internal fun TransactionScope.pinRef(observationId: String): DocumentReference =
    db.document(PinFields.COLLECTION, observationId)

internal fun TransactionScope.pinPhotoRef(observationId: String): DocumentReference =
    db.document(PinFields.COLLECTION, observationId, PinFields.PHOTO, "0")

internal fun timestampOf(millis: Long) = Timestamp(Date(millis))

internal fun TransactionScope.readCardPhotos(ownerId: String, cardId: String): Map<Int, CardPhoto> =
    (0 until CARD_PHOTO_SLOTS).mapNotNull { slot ->
        read(cardPhotoRef(ownerId, cardId, slot), CardPhotoMapper)?.let { slot to it }
    }.toMap()

internal fun cardPhotoFields(thumbnail: ByteArray, width: Int, height: Int, observationId: String, addedAt: Any): Map<String, Any?> = mapOf(
    PhotoFields.DATA to Blob.fromBytes(thumbnail),
    PhotoFields.WIDTH to width.toLong(),
    PhotoFields.HEIGHT to height.toLong(),
    CardPhotoFields.ADDED_AT to addedAt,
    CardPhotoFields.OBSERVATION_ID to observationId,
)

internal fun newCardFields(identity: CardIdentity, observedAt: Long, cardPhotoCount: Int, cardPhotoNext: Int): Map<String, Any?> {
    val first = CardMath.first(observedAt)
    return mapOf(
        JournalCardFields.TAXON_ID to identity.taxonId?.toLong(),
        JournalCardFields.COMMON_NAME to identity.commonName,
        JournalCardFields.SCIENTIFIC_NAME to identity.scientificName,
        JournalCardFields.CATEGORY to identity.category.name,
        JournalCardFields.DESCRIPTION to identity.description,
        JournalCardFields.DESCRIPTION_SOURCE to identity.descriptionSource,
        JournalCardFields.REFERENCE_PHOTO_URL to identity.referencePhotoUrl,
        JournalCardFields.REFERENCE_PHOTO_ATTRIBUTION to identity.referencePhotoAttribution,
        JournalCardFields.OBSERVATION_COUNT to first.count.toLong(),
        JournalCardFields.FIRST_OBSERVED_AT to timestampOf(first.firstObservedAt),
        JournalCardFields.LAST_OBSERVED_AT to timestampOf(first.lastObservedAt),
        JournalCardFields.PINNED to false,
        JournalCardFields.VERIFIED to false,
        JournalCardFields.VERIFICATION_STATUS to "none",
        JournalCardFields.CARD_PHOTO_COUNT to cardPhotoCount.toLong(),
        JournalCardFields.CARD_PHOTO_NEXT to cardPhotoNext.toLong(),
        JournalCardFields.CREATED_AT to FieldValue.serverTimestamp(),
        JournalCardFields.UPDATED_AT to FieldValue.serverTimestamp(),
    )
}

internal fun summaryUpdate(summary: CardSummary): Map<String, Any?> = mapOf(
    JournalCardFields.OBSERVATION_COUNT to summary.count.toLong(),
    JournalCardFields.FIRST_OBSERVED_AT to timestampOf(summary.firstObservedAt),
    JournalCardFields.LAST_OBSERVED_AT to timestampOf(summary.lastObservedAt),
    JournalCardFields.UPDATED_AT to FieldValue.serverTimestamp(),
)

internal fun JournalCard.summary() = CardSummary(observationCount, firstObservedAt, lastObservedAt)

internal fun TransactionScope.removeFromCard(
    ownerId: String,
    card: JournalCard,
    observationId: String,
    wasPublic: Boolean,
    photos: Map<Int, CardPhoto>,
    remainingFirst: Long?,
    remainingLast: Long?,
) {
    val reduced = CardMath.remove(card.summary(), remainingFirst, remainingLast)
    if (reduced == null) {
        for (slot in 0 until CARD_PHOTO_SLOTS) delete(cardPhotoRef(ownerId, card.id, slot))
        delete(cardRef(ownerId, card.id))
        return
    }
    val updates = summaryUpdate(reduced).toMutableMap()
    if (wasPublic) {
        val mine = photos.entries.firstOrNull { it.value.observationId == observationId }
        if (mine != null) {
            delete(cardPhotoRef(ownerId, card.id, mine.key))
            updates[JournalCardFields.CARD_PHOTO_COUNT] = (card.cardPhotoCount - 1).coerceAtLeast(0).toLong()
        }
    }
    update(cardRef(ownerId, card.id), updates)
}

internal fun TransactionScope.readCard(ownerId: String, cardId: String): JournalCard? =
    read(cardRef(ownerId, cardId), JournalCardMapper)

