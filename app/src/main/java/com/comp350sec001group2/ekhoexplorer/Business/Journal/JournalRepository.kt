package com.comp350sec001group2.ekhoexplorer.Business.Journal

import kotlinx.coroutines.flow.Flow

interface JournalRepository {

    /** Observe this owner's saved entries, newest first. */
    fun observeDrafts(ownerId: String): Flow<List<JournalDraft>>

    /** Return one entry belonging to this owner, or null. */
    suspend fun getDraft(
        ownerId: String,
        draftId: String,
    ): JournalDraft?

    /** Persist an entry before success; replace it when editing. */
    suspend fun saveDraft(draft: JournalDraft)

    /** Delete only the entry belonging to this owner. */
    suspend fun deleteDraft(
        ownerId: String,
        draftId: String,
    )
}
