package com.comp350sec001group2.ekhoexplorer.Business.Database.Queries

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Observation
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.ObservationFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.ObservationMapper

class GetUserObservationsQuery(
    private val userId: String
) : FirestoreQuery<List<Observation>>() {

    override suspend fun run(db: FirestoreContext): List<Observation> {
        val query = db.collection(ObservationFields.COLLECTION)
            .whereEqualTo(ObservationFields.USER_ID, userId)

        return db.getDocuments(query, 100, ObservationMapper)
    }
}
