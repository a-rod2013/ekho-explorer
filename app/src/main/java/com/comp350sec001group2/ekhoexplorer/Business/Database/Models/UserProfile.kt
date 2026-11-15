package com.comp350sec001group2.ekhoexplorer.Business.Database.Models

import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentData
import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentReader
import java.util.Date

data class UserProfile(
    val userId: String,
    val username: String,
    val bio: String = "",
    val createdAt: Date? = null,
    val permission: String = "user",
)

object UserProfileFields {
    const val COLLECTION = "users"
    const val USERNAME = "username"
    const val USERNAME_LOWER = "usernameLower"
    const val CREATED_AT = "createdAt"

    const val BIO = "bio"
    const val PERMISSION = "permission"
}

object UserProfileMapper : DocumentReader<UserProfile> {
    override fun read(doc: DocumentData) = UserProfile(
        userId = doc.id,
        username = doc.string(UserProfileFields.USERNAME),
        bio = doc.stringOrNull(UserProfileFields.BIO) ?: "",
        createdAt = doc.dateOrNull(UserProfileFields.CREATED_AT),
        permission = doc.stringOrNull(UserProfileFields.PERMISSION) ?: "user",
    )
}
