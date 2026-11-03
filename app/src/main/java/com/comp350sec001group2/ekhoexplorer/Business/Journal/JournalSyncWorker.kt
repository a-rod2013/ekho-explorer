package com.comp350sec001group2.ekhoexplorer.Business.Journal

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.minutes

class JournalSyncWorker(
    appContext: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(appContext, parameters) {

    override suspend fun doWork(): Result {
        val ownerId = inputData.getString(OWNER_ID)
            ?: return Result.failure(
                workDataOf("error" to "Missing journal owner")
            )

        val container = (applicationContext as EkhoApp).container

        if (!container.journalIdentity.isCurrentOwner(ownerId)) {
            // Keep entries pending. Signing in schedules another attempt.
            return Result.success()
        }

        return try {
            val outcome = withTimeoutOrNull(2.minutes) {
                container.journalSyncCoordinator.sync(ownerId)
            }

            when (outcome) {
                JournalSyncOutcome.COMPLETE,
                JournalSyncOutcome.ACCOUNT_CHANGED -> Result.success()

                JournalSyncOutcome.MORE_PENDING,
                null -> Result.retry()

                JournalSyncOutcome.CONFLICT -> {
                    Log.w(
                        "JournalSync",
                        "Journal revision conflict",
                    )

                    Result.failure(
                        workDataOf(
                            "error" to "Journal revision conflict"
                        )
                    )
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (firesStoneFailure: FirestoreException) {
            Log.w(
                "JournalSync",
                "Sync failed: ${firesStoneFailure.kind}",
                firesStoneFailure,
            )

            when (firesStoneFailure.kind) {
                FirestoreErrorKind.UNAVAILABLE,
                FirestoreErrorKind.TIMEOUT -> Result.retry()

                else -> Result.failure(
                    workDataOf(
                        "error" to firesStoneFailure.kind.toString())
                )
            }
        } catch (unexpectedFailure: Exception) {
            Log.e(
                "JournalSync",
                "Unexpected sync failure",
                unexpectedFailure,
            )

            Result.failure(
                workDataOf(
                    "error" to "Unexpected journal sync failure"
                )
            )
        }
    }

    companion object {
        const val OWNER_ID = "ownerId"
    }
}
