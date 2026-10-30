package com.comp350sec001group2.ekhoexplorer.Business.Database.Models

import com.comp350sec001group2.ekhoexplorer.Business.Database.Transactions.VoteMath

data class Comment(
    val id: String,
    val postId: String,
    val userId: String,
    val username: String,
    val avatarUrl: String?,
    val text: String,
    val upCount: Int,
    val downCount: Int,
    val myVote: Vote = Vote.NONE,
) {
    /** Returns the comment after the user taps [tapped]. Tapping the active vote clears it. */
    fun withVote(tapped: Vote): Comment {
        val next = VoteMath.next(myVote, tapped)
        val (upChange, downChange) = VoteMath.deltas(myVote, next)
        return copy(
            upCount = maxOf(upCount + upChange, 0),
            downCount = maxOf(downCount + downChange, 0),
            myVote = next,
        )
    }
}

data class CommentVote(val commentId: String, val value: Vote)
