package com.comp350sec001group2.ekhoexplorer.Business.Database.Models

import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentData
import com.comp350sec001group2.ekhoexplorer.Business.Database.DocumentReader
import java.time.Instant

enum class SupportRequestStatus { PENDING, APPROVED, REJECTED }

data class UsernameChangeRequest(
    val userId: String,
    val currentUsername: String,
    val requestedUsername: String,
    val reason: String,
    val status: SupportRequestStatus,
    val adminNote: String?,
    val createdAt: Instant?,
)

object SupportRequestFields {
    const val COLLECTION = "supportRequests"
    const val USER_ID = "userId"
    const val TYPE = "type"
    const val TYPE_USERNAME_CHANGE = "usernameChange"
    const val CURRENT_USERNAME = "currentUsername"
    const val REQUESTED_USERNAME = "requestedUsername"
    const val REASON = "reason"
    const val STATUS = "status"
    const val CREATED_AT = "createdAt"
    const val RESOLVED_BY = "resolvedBy"
    const val RESOLVED_AT = "resolvedAt"
    const val ADMIN_NOTE = "adminNote"

    fun usernameChangeId(userId: String) = "${userId}_usernameChange"

    fun statusFrom(raw: String?): SupportRequestStatus = when (raw) {
        "approved" -> SupportRequestStatus.APPROVED
        "rejected" -> SupportRequestStatus.REJECTED
        else -> SupportRequestStatus.PENDING
    }
}

object UsernameChangeRequestMapper : DocumentReader<UsernameChangeRequest> {
    override fun read(doc: DocumentData) = UsernameChangeRequest(
        userId = doc.string(SupportRequestFields.USER_ID),
        currentUsername = doc.string(SupportRequestFields.CURRENT_USERNAME),
        requestedUsername = doc.string(SupportRequestFields.REQUESTED_USERNAME),
        reason = doc.stringOrNull(SupportRequestFields.REASON).orEmpty(),
        status = SupportRequestFields.statusFrom(doc.stringOrNull(SupportRequestFields.STATUS)),
        adminNote = doc.stringOrNull(SupportRequestFields.ADMIN_NOTE),
        createdAt = doc.instantOrNull(SupportRequestFields.CREATED_AT),
    )
}
