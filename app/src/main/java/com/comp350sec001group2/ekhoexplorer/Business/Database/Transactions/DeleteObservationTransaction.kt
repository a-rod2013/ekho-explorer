package com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.JournalObservationMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.TransactionScope
import com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards.CardMath

class DeleteObservationTransaction(
    private val ownerId: String,
    private val observationId: String,
    private val remainingFirst: Long?,
    private val remainingLast: Long?,
) : FirestoreTransaction<Boolean>() {

    override fun run(tx: TransactionScope): Boolean {
        val observation = tx.read(tx.observationRef(ownerId, observationId), JournalObservationMapper)   // reads first
            ?: throw missingJournal("Observation $observationId")
        val card = tx.readCard(ownerId, observation.cardId) ?: throw missingJournal("Card ${observation.cardId}")
        val photos = if (observation.isPublic) tx.readCardPhotos(ownerId, card.id) else emptyMap()

        for (index in 0 until observation.photoCount) tx.delete(tx.observationPhotoRef(ownerId, observationId, index))   // then writes
        tx.delete(tx.observationRef(ownerId, observationId))
        if (observation.hasPin) {
            tx.delete(tx.pinRef(observationId))
            if (observation.isPublic) tx.delete(tx.pinPhotoRef(observationId))
        }
        tx.removeFromCard(ownerId, card, observationId, observation.isPublic, photos, remainingFirst, remainingLast)
        return CardMath.remove(card.summary(), remainingFirst, remainingLast) == null
    }
}
