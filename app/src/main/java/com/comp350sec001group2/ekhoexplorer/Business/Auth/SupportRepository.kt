package com.comp350sec001group2.ekhoexplorer.Business.Auth

import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.GetUsernameChangeRequestQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.RequestUsernameChangeCommand
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreService
import com.comp350sec001group2.ekhoexplorer.Business.Database.Queries.GetUserProfileQuery
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.UsernameChangeRequest

interface SupportRepository {
    suspend fun usernameChangeRequest(userId: String): UsernameChangeRequest?

    suspend fun currentUsername(userId: String): String?

    suspend fun requestUsernameChange(userId: String, currentUsername: String, requestedUsername: String, reason: String)
}

class FirestoreSupportRepository(private val service: FirestoreService) : SupportRepository {

    override suspend fun usernameChangeRequest(userId: String): UsernameChangeRequest? =
        service.execute(GetUsernameChangeRequestQuery(userId))

    override suspend fun currentUsername(userId: String): String? =
        service.execute(GetUserProfileQuery(userId))?.username

    override suspend fun requestUsernameChange(
        userId: String,
        currentUsername: String,
        requestedUsername: String,
        reason: String,
    ) {
        service.execute(RequestUsernameChangeCommand(userId, currentUsername, requestedUsername, reason))
    }
}
