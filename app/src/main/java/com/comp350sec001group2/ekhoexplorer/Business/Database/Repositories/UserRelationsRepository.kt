package com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories

import com.comp350sec001group2.ekhoexplorer.Business.Cache.ActiveUser
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.SetUserRelationCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreService
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.RelationType
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetUserRelationsQuery

/** Blocking users and hiding their posts. The result lives in [EhkoCache.activeUser]. */
interface UserRelationsRepository {
    /** Fills the active user's blocked / hidden lists from the database. Call after sign-in. */
    suspend fun load()

    /** Hide this user's posts and comments. */
    suspend fun block(targetUserId: String)

    /** Hide this user's posts. Their comments stay visible. */
    suspend fun hidePostsFrom(targetUserId: String)
}

class FirestoreUserRelationsRepository(
    private val service: FirestoreService,
) : UserRelationsRepository {

    override suspend fun load() {
        val me = EhkoCache.activeUser
        if (!me.isSignedIn) return
        val relations = service.execute(GetUserRelationsQuery(me.userId))
        EhkoCache.updateActiveUser { user ->
            user.copy(
                blockedUserIds = relations.filter { it.type == RelationType.BLOCKED }.mapTo(HashSet()) { it.targetUserId },
                hiddenUserIds = relations.filter { it.type == RelationType.HIDDEN }.mapTo(HashSet()) { it.targetUserId },
            )
        }
    }

    override suspend fun block(targetUserId: String) = set(targetUserId, RelationType.BLOCKED)

    override suspend fun hidePostsFrom(targetUserId: String) = set(targetUserId, RelationType.HIDDEN)

    // Update the cache immediately, redirect, and then confirm with the database.
    private suspend fun set(targetUserId: String, type: RelationType) {
        val me = EhkoCache.activeUser
        if (!me.isSignedIn || targetUserId == me.userId) return

        val alreadyDone = when (type) {
            RelationType.BLOCKED -> targetUserId in me.blockedUserIds
            RelationType.HIDDEN -> targetUserId in me.hiddenUserIds
        }
        if (alreadyDone) return

        EhkoCache.updateActiveUser { user -> user.with(type, targetUserId) }
        try {
            service.execute(SetUserRelationCommand(me.userId, targetUserId, type))
        } catch (e: FirestoreException) {
            EhkoCache.updateActiveUser { user -> user.without(type, targetUserId) }   // undo
            throw e
        }
    }

    private fun ActiveUser.with(type: RelationType, id: String) =
        when (type) {
            RelationType.BLOCKED -> copy(blockedUserIds = blockedUserIds + id)
            RelationType.HIDDEN -> copy(hiddenUserIds = hiddenUserIds + id)
        }

    private fun ActiveUser.without(type: RelationType, id: String) =
        when (type) {
            RelationType.BLOCKED -> copy(blockedUserIds = blockedUserIds - id)
            RelationType.HIDDEN -> copy(hiddenUserIds = hiddenUserIds - id)
        }
}
