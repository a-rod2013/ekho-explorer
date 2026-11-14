package com.comp350sec001group2.ekhoexplorer.Business.Database.Queries

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UserAvatar
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UserAvatarFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UserAvatarMapper
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UserProfileFields

class GetUserAvatarQuery (
    private val userId: String
) : FirestoreQuery<UserAvatar?>() {

    override suspend fun run(db: FirestoreContext): UserAvatar? =
        db.getDocument(
            db.document(
                UserProfileFields.COLLECTION,
                userId,
                UserAvatarFields.COLLECTION,
                UserAvatarFields.DOCUMENT
            ),
            UserAvatarMapper
        )
}
