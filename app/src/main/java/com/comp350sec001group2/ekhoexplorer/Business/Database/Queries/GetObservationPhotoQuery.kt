
package com.comp350sec001group2.ekhoexplorer.Business.Database.Queries

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.ObservationFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PhotoFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostPhoto
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.PostPhotoMapper

/**
 * Photo [index] of observation [observationId], or null if there is none.
 */
class GetObservationPhotoQuery(
    private val observationId: String,
    private val index: Int,
) : FirestoreQuery<PostPhoto?>() {

    override suspend fun run(db: FirestoreContext): PostPhoto? =
        db.getDocument(
            db.document(
                ObservationFields.COLLECTION,
                observationId,
                PhotoFields.COLLECTION,
                index.toString()
            ),
            PostPhotoMapper,
        )
}
