package com.comp350sec001group2.ekhoexplorer.Business.Database.Models

import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentData
import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentReader


object PostMapper : DocumentReader<Post> {
    override fun read(doc: DocumentData) = Post(
        id = doc.id,
        username = doc.string(PostFields.USERNAME),
        avatarUrl = doc.stringOrNull(PostFields.AVATAR_URL),
        imageUrls = doc.stringList(PostFields.IMAGE_URLS),
        caption = doc.stringOrNull(PostFields.CAPTION) ?: "",
        upCount = doc.longOrNull(PostFields.UP_COUNT)?.toInt() ?: 0,
        downCount = doc.longOrNull(PostFields.DOWN_COUNT)?.toInt() ?: 0,
        myVote = Vote.NONE,        // filled in later, see GetFeedVotesQuery
    )
}
