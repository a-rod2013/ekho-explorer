package com.comp350sec001group2.ekhoexplorer.Business.Database.Models

import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentData
import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentReader

/**
 * The user's profile picture stored at:
 * users/{userId}/avatar/current
 */

data class UserAvatar(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
)

object UserAvatarFields {
    const val COLLECTION = "avatar"
    const val DOCUMENT = "current"

    const val DATA = "data"
    const val WIDTH = "width"
    const val HEIGHT = "height"
}

object UserAvatarMapper : DocumentReader<UserAvatar> {
    override fun read(doc: DocumentData) = UserAvatar(
        bytes = doc.bytes(UserAvatarFields.DATA),
        width = doc.int(UserAvatarFields.WIDTH),
        height = doc.int(UserAvatarFields.HEIGHT)
    )
}
