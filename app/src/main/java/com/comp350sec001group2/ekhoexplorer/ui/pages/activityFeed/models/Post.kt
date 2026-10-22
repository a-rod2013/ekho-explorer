package com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.models

enum class Vote{NONE, UP, DOWN}

data class Post(
    val id: String,
    val username: String,
    val avatarUrl: String?,
    val imageUrls: List<String>,
    val caption: String,
    val upCount: Int,
    val downCount: Int,
    val myVote: Vote = Vote.NONE,
) {
    /** Returns the post after the user taps [tapped], Tapping the active vote clears it. */
    fun withVote(tapped: Vote): Post {
        if(tapped == Vote.NONE) return this

        val next = if(tapped == myVote) Vote.NONE else tapped

        var up = upCount
        var down = downCount

        when(myVote) {
            Vote.UP -> up--
            Vote.DOWN -> down--
            Vote.NONE -> Unit
        }

        when(next) {
            Vote.UP -> up++
            Vote.DOWN -> down++
            Vote.NONE -> Unit
        }

        return copy(upCount = maxOf(up, 0), downCount = maxOf(down, 0), myVote = next)
    }
}
