package com.comp350sec001group2.ekhoexplorer.Business.Database.Models

object CommentFields {
    const val COLLECTION = "comments"
    const val POST_ID = "postId"
    const val USER_ID = "userId"
    const val USERNAME = "username"
    const val AVATAR_URL = "avatarUrl"
    const val TEXT = "text"
    const val UP_COUNT = "upCount"
    const val DOWN_COUNT = "downCount"
    const val CREATED_AT = "createdAt"

    const val MAX_TEXT_LENGTH = 500
}

/** One document per (comment, user): commentVotes/{commentId}_{userId}. */
object CommentVoteFields {
    const val COLLECTION = "commentVotes"
    const val COMMENT_ID = "commentId"
    const val POST_ID = "postId"
    const val USER_ID = "userId"
    const val VALUE = "value"
}
