package com.comp350sec001group2.ekhoexplorer.Business.Database.Models

import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentData
import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentReader

/**
 * The public face of an account: users/{uid}.
 */
data class UserProfile(
    val userId: String,
    val username: String,
)

object UserProfileFields {
    const val COLLECTION = "users"
    const val USERNAME = "username"
    const val USERNAME_LOWER = "usernameLower"
    const val CREATED_AT = "createdAt"
}

object UserProfileMapper : DocumentReader<UserProfile> {
    override fun read(doc: DocumentData) = UserProfile(
        userId = doc.id,
        username = doc.string(UserProfileFields.USERNAME),
    )
}
