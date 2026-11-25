package com.comp350sec001group2.ekhoexplorer.Business.Database.Models

import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentData
import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentReader
import com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards.CardIds
import com.comp350sec001group2.ekhoexplorer.Business.Journal.Cards.JournalCategory

object JournalCardFields {
    const val USERS = "users"
    const val COLLECTION = "journalCards"
    const val CARD_PHOTOS = "cardPhotos"
    const val NOTES = "notes"

    const val TAXON_ID = "taxonId"
    const val COMMON_NAME = "commonName"
    const val SCIENTIFIC_NAME = "scientificName"
    const val CATEGORY = "category"
    const val DESCRIPTION = "description"
    const val DESCRIPTION_SOURCE = "descriptionSource"
    const val REFERENCE_PHOTO_URL = "referencePhotoUrl"
    const val REFERENCE_PHOTO_ATTRIBUTION = "referencePhotoAttribution"
    const val OBSERVATION_COUNT = "observationCount"
    const val FIRST_OBSERVED_AT = "firstObservedAt"
    const val LAST_OBSERVED_AT = "lastObservedAt"
    const val PINNED = "pinned"
    const val VERIFIED = "verified"
    const val VERIFICATION_STATUS = "verificationStatus"
    const val CARD_PHOTO_COUNT = "cardPhotoCount"
    const val CARD_PHOTO_NEXT = "cardPhotoNext"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"
    const val JOURNAL_PRIVATE = "journalPrivate"
}

object CardPhotoFields {
    const val ADDED_AT = "addedAt"
    const val OBSERVATION_ID = "observationId"
}

object NoteFields {
    const val TEXT = "text"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"
}

object JournalObservationFields {
    const val COLLECTION = "journalObservations"
    const val CARD_ID = "cardId"
    const val OBSERVED_AT = "observedAt"
    const val VISIBILITY = "visibility"
    const val PHOTO_COUNT = "photoCount"
    const val LATITUDE = "latitude"
    const val LONGITUDE = "longitude"
    const val HAS_PIN = "hasPin"
    const val CREATED_AT = "createdAt"
    const val PUBLIC = "PUBLIC"
    const val PRIVATE = "PRIVATE"
}

object PinFields {
    const val COLLECTION = "pins"
    const val PHOTO = "photo"
    const val OWNER_ID = "ownerId"
    const val CARD_ID = "cardId"
    const val TAXON_ID = "taxonId"
    const val COMMON_NAME = "commonName"
    const val CATEGORY = "category"
    const val LATITUDE = "latitude"
    const val LONGITUDE = "longitude"
    const val VISIBILITY = "visibility"
    const val CREATED_AT = "createdAt"
    const val EXPIRES_AT = "expiresAt"
    const val GEOHASH = "geohash"
}

data class JournalCard(
    val id: String,
    val taxonId: Int?,
    val commonName: String,
    val scientificName: String,
    val category: JournalCategory,
    val description: String,
    val descriptionSource: String,
    val referencePhotoUrl: String?,
    val referencePhotoAttribution: String?,
    val observationCount: Int,
    val firstObservedAt: Long,
    val lastObservedAt: Long,
    val pinned: Boolean,
    val verified: Boolean,
    val verificationStatus: String,
    val cardPhotoCount: Int,
    val cardPhotoNext: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    val isUnknown: Boolean get() = CardIds.isUnknown(id)
}

data class JournalObservation(
    val id: String,
    val cardId: String,
    val observedAt: Long,
    val isPublic: Boolean,
    val photoCount: Int,
    val latitude: Double?,
    val longitude: Double?,
    val hasPin: Boolean,
    val createdAtMillis: Long,
)

data class JournalNote(
    val id: String,
    val text: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class MapPin(
    val id: String,
    val ownerId: String,
    val cardId: String,
    val taxonId: Int?,
    val commonName: String,
    val category: JournalCategory,
    val latitude: Double,
    val longitude: Double,
    val isPublic: Boolean,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
)

class CardPhoto(
    val index: Int,
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
    val addedAtMillis: Long,
    val observationId: String,
)

private fun DocumentData.category(name: String): JournalCategory {
    val raw = string(name)
    return JournalCategory.entries.firstOrNull { it.name == raw }
        ?: throw mismatch("field '$name' has unknown category '$raw'")
}

private fun DocumentData.millis(name: String): Long = instantOrNull(name)?.toEpochMilli() ?: 0L

object JournalCardMapper : DocumentReader<JournalCard> {
    override fun read(doc: DocumentData) = JournalCard(
        id = doc.id,
        taxonId = doc.longOrNull(JournalCardFields.TAXON_ID)?.toInt(),
        commonName = doc.stringOrNull(JournalCardFields.COMMON_NAME) ?: "",
        scientificName = doc.stringOrNull(JournalCardFields.SCIENTIFIC_NAME) ?: "",
        category = doc.category(JournalCardFields.CATEGORY),
        description = doc.stringOrNull(JournalCardFields.DESCRIPTION) ?: "",
        descriptionSource = doc.stringOrNull(JournalCardFields.DESCRIPTION_SOURCE) ?: "",
        referencePhotoUrl = doc.stringOrNull(JournalCardFields.REFERENCE_PHOTO_URL),
        referencePhotoAttribution = doc.stringOrNull(JournalCardFields.REFERENCE_PHOTO_ATTRIBUTION),
        observationCount = doc.int(JournalCardFields.OBSERVATION_COUNT),
        firstObservedAt = doc.millis(JournalCardFields.FIRST_OBSERVED_AT),
        lastObservedAt = doc.millis(JournalCardFields.LAST_OBSERVED_AT),
        pinned = doc.booleanOrNull(JournalCardFields.PINNED) ?: false,
        verified = doc.booleanOrNull(JournalCardFields.VERIFIED) ?: false,
        verificationStatus = doc.stringOrNull(JournalCardFields.VERIFICATION_STATUS) ?: "none",
        cardPhotoCount = doc.longOrNull(JournalCardFields.CARD_PHOTO_COUNT)?.toInt() ?: 0,
        cardPhotoNext = doc.longOrNull(JournalCardFields.CARD_PHOTO_NEXT)?.toInt() ?: 0,
        createdAtMillis = doc.millis(JournalCardFields.CREATED_AT),
        updatedAtMillis = doc.millis(JournalCardFields.UPDATED_AT),
    )
}

object JournalObservationMapper : DocumentReader<JournalObservation> {
    override fun read(doc: DocumentData) = JournalObservation(
        id = doc.id,
        cardId = doc.string(JournalObservationFields.CARD_ID),
        observedAt = doc.millis(JournalObservationFields.OBSERVED_AT),
        isPublic = doc.string(JournalObservationFields.VISIBILITY) == JournalObservationFields.PUBLIC,
        photoCount = doc.int(JournalObservationFields.PHOTO_COUNT),
        latitude = doc.doubleOrNull(JournalObservationFields.LATITUDE),
        longitude = doc.doubleOrNull(JournalObservationFields.LONGITUDE),
        hasPin = doc.booleanOrNull(JournalObservationFields.HAS_PIN) ?: false,
        createdAtMillis = doc.millis(JournalObservationFields.CREATED_AT),
    )
}

object JournalNoteMapper : DocumentReader<JournalNote> {
    override fun read(doc: DocumentData) = JournalNote(
        id = doc.id,
        text = doc.stringOrNull(NoteFields.TEXT) ?: "",
        createdAtMillis = doc.millis(NoteFields.CREATED_AT),
        updatedAtMillis = doc.millis(NoteFields.UPDATED_AT),
    )
}

object MapPinMapper : DocumentReader<MapPin> {
    override fun read(doc: DocumentData) = MapPin(
        id = doc.id,
        ownerId = doc.string(PinFields.OWNER_ID),
        cardId = doc.string(PinFields.CARD_ID),
        taxonId = doc.longOrNull(PinFields.TAXON_ID)?.toInt(),
        commonName = doc.stringOrNull(PinFields.COMMON_NAME) ?: "",
        category = doc.category(PinFields.CATEGORY),
        latitude = doc.double(PinFields.LATITUDE),
        longitude = doc.double(PinFields.LONGITUDE),
        isPublic = doc.string(PinFields.VISIBILITY) == JournalObservationFields.PUBLIC,
        createdAtMillis = doc.millis(PinFields.CREATED_AT),
        expiresAtMillis = doc.millis(PinFields.EXPIRES_AT),
    )
}

object CardPhotoMapper : DocumentReader<CardPhoto> {
    override fun read(doc: DocumentData) = CardPhoto(
        index = doc.id.toIntOrNull() ?: throw doc.mismatch("card photo id '${doc.id}' is not a position"),
        bytes = doc.bytes(PhotoFields.DATA),
        width = doc.int(PhotoFields.WIDTH),
        height = doc.int(PhotoFields.HEIGHT),
        addedAtMillis = doc.millis(CardPhotoFields.ADDED_AT),
        observationId = doc.stringOrNull(CardPhotoFields.OBSERVATION_ID) ?: "",
    )
}
