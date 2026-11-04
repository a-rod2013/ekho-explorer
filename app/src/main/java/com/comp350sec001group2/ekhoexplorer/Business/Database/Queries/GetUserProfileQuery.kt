package com.comp350sec001group2.ekhoexplorer.Business.Database.Queries

import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreContext
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UserProfile
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UserProfileFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UserProfileMapper

/** The profile for [userId], or null if that account has none yet. */
class GetUserProfileQuery(private val userId: String) : FirestoreQuery<UserProfile?>() {

    override suspend fun run(db: FirestoreContext): UserProfile? =
        db.getDocument(db.document(UserProfileFields.COLLECTION, userId), UserProfileMapper)
}
