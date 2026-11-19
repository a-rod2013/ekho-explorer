
package com.comp350sec001group2.ekhoexplorer.Business.Photos

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreService
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetObservationPhotoQuery

class FirestoreObservationPhotoStore(
    private val service: FirestoreService
) : PhotoStore {

    override suspend fun read(
        observationId: String,
        index: Int
    ): ByteArray? =
        service.execute(
            GetObservationPhotoQuery(observationId, index)
        )?.bytes
}
