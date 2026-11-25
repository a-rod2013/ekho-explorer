package com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalCardFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalObservationFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalObservationMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PinFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.TransactionScope
import com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards.CardIdentity
import com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards.CardIds
import com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards.CardMath
import com.google.firebase.firestore.FieldValue

class IdentifyObservationTransaction(
    private val ownerId: String,
    private val observationId: String,
    private val target: CardIdentity,
    private val sourceRemainingFirst: Long?,
    private val sourceRemainingLast: Long?,
) : FirestoreTransaction<Unit>() {

    override fun run(tx: TransactionScope) {
        if (target.taxonId == null || CardIds.isUnknown(target.cardId) || target.cardId != CardIds.forTaxon(target.taxonId)) {
            throw invalidJournal("An observation can only be identified as a real species")
        }

        val observation = tx.read(tx.observationRef(ownerId, observationId), JournalObservationMapper)   // reads first
            ?: throw missingJournal("Observation $observationId")
        if (!CardIds.isUnknown(observation.cardId)) throw invalidJournal("Only an unidentified observation can be identified")
        val source = tx.readCard(ownerId, observation.cardId) ?: throw missingJournal("Card ${observation.cardId}")
        val existing = tx.readCard(ownerId, target.cardId)
        val sourcePhotos = if (observation.isPublic) tx.readCardPhotos(ownerId, source.id) else emptyMap()
        val movingPhoto = sourcePhotos.values.firstOrNull { it.observationId == observationId }

        // writes
        tx.update(tx.observationRef(ownerId, observationId), mapOf(JournalObservationFields.CARD_ID to target.cardId))
        if (observation.hasPin) {
            tx.update(
                tx.pinRef(observationId),
                mapOf(
                    PinFields.CARD_ID to target.cardId,
                    PinFields.TAXON_ID to target.taxonId.toLong(),
                    PinFields.COMMON_NAME to target.commonName,
                    PinFields.CATEGORY to target.category.name,
                ),
            )
        }

        tx.removeFromCard(ownerId, source, observationId, observation.isPublic, sourcePhotos, sourceRemainingFirst, sourceRemainingLast)

        // The species card. The card photo is copied into the target's next slot, keeping its added time.
        val copy = movingPhoto != null
        val slot = (existing?.cardPhotoNext ?: 0) % CARD_PHOTO_SLOTS
        val photoCount = if (copy) minOf((existing?.cardPhotoCount ?: 0) + 1, CARD_PHOTO_SLOTS) else existing?.cardPhotoCount ?: 0
        val photoNext = if (copy) (existing?.cardPhotoNext ?: 0) + 1 else existing?.cardPhotoNext ?: 0
        if (existing == null) {
            tx.set(tx.cardRef(ownerId, target.cardId), newCardFields(target, observation.observedAt, photoCount, photoNext))
        } else {
            val updates = summaryUpdate(CardMath.add(existing.summary(), observation.observedAt)).toMutableMap()
            if (copy) {
                updates[JournalCardFields.CARD_PHOTO_COUNT] = photoCount.toLong()
                updates[JournalCardFields.CARD_PHOTO_NEXT] = photoNext.toLong()
            }
            tx.update(tx.cardRef(ownerId, target.cardId), updates)
        }
        if (movingPhoto != null) {
            tx.set(
                tx.cardPhotoRef(ownerId, target.cardId, slot),
                cardPhotoFields(movingPhoto.bytes, movingPhoto.width, movingPhoto.height, observationId, timestampOf(movingPhoto.addedAtMillis)),
            )
        }
    }
}
