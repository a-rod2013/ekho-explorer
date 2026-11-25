package com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions

import com.comp350sec001group2.ekhoexplorer.Business.Database.Batches.photoFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalCardFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalObservationFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PhotoFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PinFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.TransactionScope
import com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards.CardIdentity
import com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards.CardIds
import com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards.CardMath
import com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards.GeoPoint
import com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards.Geohash
import com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards.PinRules
import com.comp350sec001group2.ekhoexplorer.Business.Photos.EncodedPhoto
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FieldValue

class RecordObservationTransaction(
    private val ownerId: String,
    private val observationId: String,
    private val card: CardIdentity,
    private val observedAt: Long,
    private val isPublic: Boolean,
    private val location: GeoPoint?,
    private val photos: List<EncodedPhoto>,
    private val thumbnail: EncodedPhoto,
    private val clock: () -> Long = System::currentTimeMillis,
) : FirestoreTransaction<String>() {

    override fun run(tx: TransactionScope): String {
        validate()

        val existing = tx.readCard(ownerId, card.cardId)                       // reads first

        val visibility = if (isPublic) JournalObservationFields.PUBLIC else JournalObservationFields.PRIVATE
        val observation = mutableMapOf<String, Any?>(                          // then writes
            JournalObservationFields.CARD_ID to card.cardId,
            JournalObservationFields.OBSERVED_AT to timestampOf(observedAt),
            JournalObservationFields.VISIBILITY to visibility,
            JournalObservationFields.PHOTO_COUNT to photos.size.toLong(),
            JournalObservationFields.HAS_PIN to (location != null),
            JournalObservationFields.CREATED_AT to FieldValue.serverTimestamp(),
        )
        if (location != null) {
            observation[JournalObservationFields.LATITUDE] = location.latitude
            observation[JournalObservationFields.LONGITUDE] = location.longitude
        }
        tx.set(tx.observationRef(ownerId, observationId), observation)
        photos.forEachIndexed { index, photo -> tx.set(tx.observationPhotoRef(ownerId, observationId, index), photoFields(photo)) }

        val slot = (existing?.cardPhotoNext ?: 0) % CARD_PHOTO_SLOTS
        val photoCount = if (isPublic) minOf((existing?.cardPhotoCount ?: 0) + 1, CARD_PHOTO_SLOTS) else existing?.cardPhotoCount ?: 0
        val photoNext = if (isPublic) (existing?.cardPhotoNext ?: 0) + 1 else existing?.cardPhotoNext ?: 0
        if (existing == null) {
            tx.set(tx.cardRef(ownerId, card.cardId), newCardFields(card, observedAt, photoCount, photoNext))
        } else {
            val updates = summaryUpdate(CardMath.add(existing.summary(), observedAt)).toMutableMap()
            if (isPublic) {
                updates[JournalCardFields.CARD_PHOTO_COUNT] = photoCount.toLong()
                updates[JournalCardFields.CARD_PHOTO_NEXT] = photoNext.toLong()
            }
            tx.update(tx.cardRef(ownerId, card.cardId), updates)
        }
        if (isPublic) {
            tx.set(
                tx.cardPhotoRef(ownerId, card.cardId, slot),
                cardPhotoFields(thumbnail.bytes, thumbnail.width, thumbnail.height, observationId, FieldValue.serverTimestamp()),
            )
        }

        if (location != null) {
            val expiresAt = timestampOf(PinRules.expiresAt(card.category, clock()))
            val latitude = if (isPublic) PinRules.roundForPublic(location.latitude) else location.latitude
            val longitude = if (isPublic) PinRules.roundForPublic(location.longitude) else location.longitude
            tx.set(
                tx.pinRef(observationId),
                mapOf(
                    PinFields.OWNER_ID to ownerId,
                    PinFields.CARD_ID to card.cardId,
                    PinFields.TAXON_ID to card.taxonId?.toLong(),
                    PinFields.COMMON_NAME to card.commonName,
                    PinFields.CATEGORY to card.category.name,
                    PinFields.LATITUDE to latitude,
                    PinFields.LONGITUDE to longitude,
                    PinFields.VISIBILITY to visibility,
                    PinFields.CREATED_AT to FieldValue.serverTimestamp(),
                    PinFields.EXPIRES_AT to expiresAt,
                    PinFields.GEOHASH to Geohash.encode(latitude, longitude),
                ),
            )
            if (isPublic) {
                tx.set(
                    tx.pinPhotoRef(observationId),
                    mapOf(
                        PhotoFields.DATA to Blob.fromBytes(thumbnail.bytes),
                        PhotoFields.WIDTH to thumbnail.width.toLong(),
                        PhotoFields.HEIGHT to thumbnail.height.toLong(),
                        // The picture expires with its pin (a TTL policy on this field removes it).
                        PinFields.EXPIRES_AT to expiresAt,
                    ),
                )
            }
        }
        return card.cardId
    }

    private fun validate() {
        if (observationId.isBlank()) throw invalidJournal("An observation needs an id")
        if (photos.size !in 1..MAX_OBSERVATION_PHOTOS) throw invalidJournal("An observation needs 1 to $MAX_OBSERVATION_PHOTOS photos but had ${photos.size}")
        if (photos.any { it.sizeBytes > MAX_STORED_BYTES } || thumbnail.sizeBytes > MAX_STORED_BYTES) {
            throw invalidJournal("A photo is over the $MAX_STORED_BYTES byte cap")
        }
        if (observedAt <= 0) throw invalidJournal("The observation date is not valid")
        if (location != null && !location.isValid) throw invalidJournal("The location is not valid")
        val matches = if (card.taxonId != null) card.cardId == CardIds.forTaxon(card.taxonId)
        else card.cardId == CardIds.unknown(card.category)
        if (!matches) throw invalidJournal("Card id ${card.cardId} does not match the species")
    }

    private companion object {
        const val MAX_OBSERVATION_PHOTOS = 3
    }
}
