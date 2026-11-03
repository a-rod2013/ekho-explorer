package com.comp350sec001group2.ekhoexplorer.Business.Journal

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class LocalJournalRepository(
    private val dao: JournalDraftDao,
    private val onDraftChanged: (String) -> Unit = {},
): JournalRepository {

    override fun observeDrafts(
        ownerId: String,
    ): Flow<List<JournalDraft>> {
        require(ownerId.isNotBlank()) { "An owner ID is required." }

        return dao.observeDrafts(ownerId)
            .map { entries -> entries.map { it.toDraft() } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getDraft(
        ownerId: String,
        draftId: String,
    ): JournalDraft? {
        require(ownerId.isNotBlank()) { "An owner ID is required." }
        require(draftId.isNotBlank()) { "A draft ID is required." }

        return dao.getDraft(ownerId, draftId)?.toDraft()
    }

    override suspend fun saveDraft(draft: JournalDraft) {
        require(draft.ownerId.isNotBlank()) { "An owner ID is required." }
        require(draft.id.isNotBlank()) { "A draft ID is required." }
        require(draft.updatedAtMillis >= draft.createdAtMillis) { "Update time cannot precede creation time." }

        dao.saveDraft(draft.toEntity())
        notifyChanged(draft.ownerId)
    }

    override suspend fun deleteDraft(
        ownerId: String,
        draftId: String,
    ) {
        require(ownerId.isNotBlank()) {
            "An owner ID is required."
        }
        require(draftId.isNotBlank()) {
            "A draft ID is required."
        }

        dao.deleteDraft(
            ownerId = ownerId,
            draftId = draftId,
            deletedAtMillis = System.currentTimeMillis()
        )
        notifyChanged(ownerId)
    }
    private fun notifyChanged(ownerId: String) {
        try {
            onDraftChanged(ownerId)
        } catch (e: Exception) {
            // The database change succeeded. Do not report it as a failed save.
            Log.w("JournalSync", "Could not schedule journal sync", e)
        }
    }
}
