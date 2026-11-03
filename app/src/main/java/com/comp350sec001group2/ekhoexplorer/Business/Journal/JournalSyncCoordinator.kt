package com.comp350sec001group2.ekhoexplorer.Business.Journal

import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.JournalUploadResult
import com.comp350sec001group2.ekhoexplorer.Business.Database.Commands.UploadJournalDraftTransaction
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreService
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class JournalSyncOutcome {
    COMPLETE,
    ACCOUNT_CHANGED,
    CONFLICT,
    MORE_PENDING,
}

class JournalSyncCoordinator(
    private val dao: JournalDraftDao,
    private val service: FirestoreService,
    private val identity: JournalIdentity,
) {
    private val mutex = Mutex()

    suspend fun sync(ownerId: String): JournalSyncOutcome = mutex.withLock {
            // Bound each run so continuous editing cannot keep it alive forever.
            repeat(10) {
                currentCoroutineContext().ensureActive()

                if (!identity.isCurrentOwner(ownerId)) {
                    return@withLock JournalSyncOutcome.ACCOUNT_CHANGED
                }

                val pending = dao.getPendingChanges(ownerId, 50)

                if (pending.isEmpty()) {
                    return@withLock JournalSyncOutcome.COMPLETE
                }

                var conflictFound = false

                for (entry in pending) {
                    currentCoroutineContext().ensureActive()

                    if (!identity.isCurrentOwner(ownerId)) {
                        return@withLock JournalSyncOutcome.ACCOUNT_CHANGED
                    }

                    val result = service.execute(
                        UploadJournalDraftTransaction(
                            draft = entry,
                            identity = identity,
                        )
                    )

                    if (!identity.isCurrentOwner(ownerId)) {
                        return@withLock JournalSyncOutcome.ACCOUNT_CHANGED
                    }

                    when (result) {
                        JournalUploadResult.WRITTEN,
                        JournalUploadResult.ALREADY_UPLOADED -> {
                            dao.acknowledgeUpload(
                                ownerId = ownerId,
                                draftId = entry.id,
                                uploadedRevision = entry.revision,
                            )
                        }

                        JournalUploadResult.CONFLICT -> {
                            conflictFound = true
                        }
                    }
                }

                if (conflictFound) {
                    return@withLock JournalSyncOutcome.CONFLICT
                }
            }

            JournalSyncOutcome.MORE_PENDING
    }
}
