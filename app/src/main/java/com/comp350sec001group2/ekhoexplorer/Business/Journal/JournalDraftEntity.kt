package com.comp350sec001group2.ekhoexplorer.Business.Journal

import androidx.room.ColumnInfo
import androidx.room.Entity
import org.json.JSONArray

@Entity(
    tableName = "journal_drafts",
    primaryKeys = ["ownerId", "id"],
)

data class JournalDraftEntity(
    val id: String,
    val ownerId: String,
    val title: String,
    val notes: String,
    val photoPathsJson: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,

    @ColumnInfo(defaultValue = "1")
    val revision: Long = 1L,

    @ColumnInfo(defaultValue = "0")
    val syncedRevision: Long = 0L,

    @ColumnInfo(defaultValue = "NULL")
    val deletedAtMillis: Long? = null,

)

internal fun JournalDraft.toEntity() = JournalDraftEntity(
    id = id,
    ownerId = ownerId,
    title = title,
    notes = notes,
    photoPathsJson = JSONArray(photoPaths).toString(),
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

internal fun JournalDraftEntity.toDraft(): JournalDraft {
    val photos = JSONArray(photoPathsJson)

    return JournalDraft(
        id = id,
        ownerId = ownerId,
        title = title,
        notes = notes,
        photoPaths = List(photos.length()) { photos.getString(it) },
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
    )
}
