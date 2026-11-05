package com.comp350sec001group2.ekhoexplorer.Business.Database.Models

import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentData
import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentReader

object VoteMapper : DocumentReader<PostVote> {
    override fun read(doc: DocumentData) =
        PostVote(doc.string("postId"), doc.enum<Vote>("value"))
}
