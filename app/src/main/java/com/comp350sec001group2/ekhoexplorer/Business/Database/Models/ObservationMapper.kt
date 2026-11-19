package com.comp350sec001group2.ekhoexplorer.Business.Database.Models

import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentData
import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentReader

object ObservationMapper : DocumentReader<Observation> {

    override fun read(doc: DocumentData) = Observation(
        id = doc.id,
        userId = doc.string(ObservationFields.USER_ID),
        commonName = doc.string(ObservationFields.COMMON_NAME),
        scientificName = doc.string(ObservationFields.SCIENTIFIC_NAME),
        category = doc.string(ObservationFields.CATEGORY),
        description = doc.string(ObservationFields.DESCRIPTION),
        latitude = doc.double(ObservationFields.LATITUDE),
        longitude = doc.double(ObservationFields.LONGITUDE),
        photoCount = doc.int(ObservationFields.PHOTO_COUNT),
        visibility = doc.string(ObservationFields.VISIBILITY),
        createdAt = doc.instantOrNull(ObservationFields.CREATED_AT)?.toEpochMilli()
    )
}
