package com.comp350sec001group2.ekhoexplorer.Business.Database.Queries

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Observation
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.ObservationFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.ObservationMapper

class GetPublicObservationsQuery : FirestoreQuery<List<Observation>>() {

    override suspend fun run(db: FirestoreContext): List<Observation> {
        val query = db.collection(ObservationFields.COLLECTION)
            .whereEqualTo(ObservationFields.VISIBILITY, ObservationFields.PUBLIC)

        return db.getDocuments(query, 100, ObservationMapper)
    }
}
