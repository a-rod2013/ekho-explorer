package com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories

import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Observation
import com.comp350sec001group2.ekhoexplorer.Business.Photos.EncodedPhoto

interface ObservationRepository {

    suspend fun addObservation(
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
    )
    suspend fun getObservations(userId: String): List<Observation>
}
