package com.comp350sec001group2.ekhoexplorer.Business.Auth

import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.CreateUserProfileCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreService
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetUserProfileQuery

interface ProfileStore {
    suspend fun create(userId: String, username: String)
    suspend fun exists(userId: String): Boolean
}

class FirestoreProfileStore(private val service: FirestoreService) : ProfileStore {

    override suspend fun create(userId: String, username: String) {
        service.execute(CreateUserProfileCommand(userId, username))
    }

    override suspend fun exists(userId: String): Boolean =
        service.execute(GetUserProfileQuery(userId)) != null
}
