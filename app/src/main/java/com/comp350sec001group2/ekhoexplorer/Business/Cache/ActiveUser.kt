package com.comp350sec001group2.ekhoexplorer.Business.Cache

data class ActiveUser(
    val userId: String,
    val userName: String,
    val avatarUrl: String? = null,
    val blockedUserIds: Set<String> = emptySet(),
    val hiddenUserIds: Set<String> = emptySet(),
) {
    val isSignedIn: Boolean get() = userId.isNotEmpty()

    fun isBlocked(userId: String): Boolean = userId in blockedUserIds

    fun hidesPostsFrom(userId: String): Boolean = userId in blockedUserIds || userId in hiddenUserIds

    companion object {
        val SignedOut = ActiveUser(userId = "", userName = "")
    }
}
