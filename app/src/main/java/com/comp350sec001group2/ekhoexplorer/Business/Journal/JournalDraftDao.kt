package com.comp350sec001group2.ekhoexplorer.Business.Journal

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class JournalDraftDao {

    @Query(
        """
            SELECT * FROM journal_drafts
            WHERE ownerId = :ownerId
            AND deletedAtMillis IS NULL
            ORDER BY updatedAtMillis DESC, id ASC
        """
    )

    abstract fun observeDrafts(
        ownerId: String,
        ): Flow<List<JournalDraftEntity>>

    @Query(
        """
            SELECT * FROM journal_drafts
            WHERE ownerId = :ownerId
            AND id = :draftId
            AND deletedAtMillis IS NULL
            LIMIT 1
        """
    )

    abstract suspend fun getDraft(
        ownerId: String,
        draftId: String,
    ): JournalDraftEntity?

    @Query(
        """
            SELECT * FROM journal_drafts
            WHERE ownerId = :ownerId AND id = :draftId
            LIMIT 1
        """
    )

    protected abstract suspend fun getRecord(
        ownerId: String,
        draftId: String,
    ): JournalDraftEntity?

    @Upsert
    protected abstract suspend fun writeRecord(
        draft: JournalDraftEntity,
    )

    @Transaction
    open suspend fun saveDraft(draft: JournalDraftEntity) {
        val existing = getRecord(draft.ownerId, draft.id)

        check(existing?.deletedAtMillis == null) {
            "This entry was deleted. Create a new entry with a new ID."
        }

        val nextRevision = if (existing == null) {
            1L
        } else {
            Math.addExact(existing.revision, 1L)
        }

        writeRecord(
            draft.copy(
                createdAtMillis =
                    existing?.createdAtMillis ?: draft.createdAtMillis,
                updatedAtMillis = maxOf(
                    draft.updatedAtMillis,
                    existing?.updatedAtMillis ?: draft.updatedAtMillis,
                ),
                revision = nextRevision,
                syncedRevision =
                    existing?.syncedRevision ?: 0L,
                deletedAtMillis = null,
            )
        )
    }

    @Query(
        """
            UPDATE journal_drafts
            SET deletedAtMillis = :deletedAtMillis,
                updatedAtMillis = MAX(updatedAtMillis, :deletedAtMillis),
                revision = revision + 1
            WHERE ownerId = :ownerId
            AND id = :draftId
            AND deletedAtMillis IS NULL
        """
    )

    abstract suspend fun deleteDraft(
        ownerId: String,
        draftId: String,
        deletedAtMillis: Long,
    )

    @Query(
        """
            SELECT * FROM journal_drafts
            WHERE ownerId = :ownerId
            AND revision > syncedRevision
            ORDER BY updatedAtMillis ASC, id ASC
            LIMIT :limit
        """
    )

    abstract suspend fun getPendingChanges(
        ownerId: String,
        limit: Int,
    ): List<JournalDraftEntity>

    @Query(
        """
            UPDATE journal_drafts
            SET syncedRevision = :uploadedRevision
            WHERE ownerId = :ownerId
            AND id = :draftId
            AND revision = :uploadedRevision
            AND syncedRevision < :uploadedRevision
        """
    )

    abstract suspend fun acknowledgeUpload(
        ownerId: String,
        draftId: String,
        uploadedRevision: Long,
    ): Int
}
