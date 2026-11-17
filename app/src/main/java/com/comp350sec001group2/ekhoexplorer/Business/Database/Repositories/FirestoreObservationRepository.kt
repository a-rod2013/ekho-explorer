
package com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories

import com.comp350sec001group2.ekhoexplorer.Business.Database.Batches.AddObservationWithPhotosBatch
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreService
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Observation
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetPublicObservationsQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetUserObservationsQuery
import com.comp350sec001group2.ekhoexplorer.Business.Photos.EncodedPhoto

class FirestoreObservationRepository(
    private val service: FirestoreService
) : ObservationRepository {

    override suspend fun addObservation(
        observationId: String,
        userId: String,
        commonName: String,
        scientificName: String,
        category: String,
        description: String,
        visibility: String,
        latitude: Double,
        longitude: Double,
        photos: List<EncodedPhoto>
    ) {
        service.execute(
            AddObservationWithPhotosBatch(
                observationId = observationId,
                userId = userId,
                commonName = commonName,
                scientificName = scientificName,
                category = category,
                description = description,
                visibility = visibility,
                latitude = latitude,
                longitude = longitude,
                photos = photos
            )
        )
    }

    override suspend fun getObservations(userId: String): List<Observation> {
        val myObservations = service.execute(GetUserObservationsQuery(userId))
        val publicObservations = service.execute(GetPublicObservationsQuery())

        return (myObservations + publicObservations)
            .distinctBy { it.id }
    }
}
