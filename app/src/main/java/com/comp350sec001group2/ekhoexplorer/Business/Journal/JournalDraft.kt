package com.comp350sec001group2.ekhoexplorer.Business.Journal

data class JournalDraft(
    val id: String,
    val ownerId: String,
    val title: String = "",
    val notes: String ="",
    val photoPaths: List<String> = emptyList(),
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
