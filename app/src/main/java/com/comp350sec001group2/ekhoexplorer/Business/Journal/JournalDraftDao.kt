package com.comp350sec001group2.ekhoexplorer.Business.Journal

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDraftDao {

    @Query(
        """
            SELECT * FROM journal_drafts
            WHERE ownerId = :ownerId
            ORDER BY updatedAtMillis DESC, id ASC
        """
    )
    fun observeDrafts(ownerId: String): Flow<List<JournalDraftEntity>>

    @Query(
        """
            SELECT * FROM journal_drafts
            WHERE ownerId = :ownerId AND id = :draftId
            LIMIT 1
        """
    )

    suspend fun getDraft(
        ownerId: String,
        draftId: String,
    ): JournalDraftEntity?

    @Upsert
    suspend fun saveDraft(draft: JournalDraftEntity)

    @Query(
        """
            DELETE FROM journal_drafts
            WHERE ownerId = :ownerId AND id = :draftId
        """
    )

    suspend fun deleteDraft(
        ownerId: String,
        draftId: String,
    )
}
