package com.comp350sec001group2.ekhoexplorer.Business.Database.Models

import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentData
import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentReader

object CommentMapper : DocumentReader<Comment> {
    override fun read(doc: DocumentData) = Comment(
        id = doc.id,
        postId = doc.string(CommentFields.POST_ID),
        userId = doc.string(CommentFields.USER_ID),
        username = doc.string(CommentFields.USERNAME),
        avatarUrl = doc.stringOrNull(CommentFields.AVATAR_URL),
        text = doc.string(CommentFields.TEXT),
        upCount = doc.longOrNull(CommentFields.UP_COUNT)?.toInt() ?: 0,
        downCount = doc.longOrNull(CommentFields.DOWN_COUNT)?.toInt() ?: 0,
        myVote = Vote.NONE,        // filled in later, see GetCommentVotesQuery
    )
}

object CommentVoteMapper : DocumentReader<CommentVote> {
    override fun read(doc: DocumentData) =
        CommentVote(doc.string(CommentVoteFields.COMMENT_ID), doc.enum<Vote>(CommentVoteFields.VALUE))
}
