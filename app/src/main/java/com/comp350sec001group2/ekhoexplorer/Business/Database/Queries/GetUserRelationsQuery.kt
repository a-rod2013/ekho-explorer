package com.comp350sec001group2.ekhoexplorer.Business.Database.Queries

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UserRelation
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UserRelationFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UserRelationMapper

/** Every block / hide entry [ownerId] has created. */
class GetUserRelationsQuery(private val ownerId: String) : FirestoreQuery<List<UserRelation>>() {

    override suspend fun run(db: FirestoreContext): List<UserRelation> {
        val query = db.collection(UserRelationFields.COLLECTION).whereEqualTo(UserRelationFields.OWNER_ID, ownerId)
        return db.getDocuments(query, FirestoreContext.MAX_READ_LIMIT, UserRelationMapper)
    }
}
