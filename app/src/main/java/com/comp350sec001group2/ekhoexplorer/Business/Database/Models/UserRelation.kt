package com.comp350sec001group2.ekhoexplorer.Business.Database.Models

import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentData
import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentReader

enum class RelationType {
    /** Hide the user's posts and comments. */
    BLOCKED,

    /** Hide the user's posts only. */
    HIDDEN,
}

data class UserRelation(val ownerId: String, val targetUserId: String, val type: RelationType)

object UserRelationFields {
    const val COLLECTION = "userRelations"
    const val OWNER_ID = "ownerId"
    const val TARGET_ID = "targetId"
    const val TYPE = "type"
    const val CREATED_AT = "createdAt"

    fun documentId(ownerId: String, targetId: String, type: RelationType) = "${ownerId}_${targetId}_${type.name}"
}

object UserRelationMapper : DocumentReader<UserRelation> {
    override fun read(doc: DocumentData) = UserRelation(
        ownerId = doc.string(UserRelationFields.OWNER_ID),
        targetUserId = doc.string(UserRelationFields.TARGET_ID),
        type = doc.enum<RelationType>(UserRelationFields.TYPE),
    )
}
