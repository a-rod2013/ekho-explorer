package com.comp350sec001group2.ekhoexplorer.Business.Journal

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class JournalSyncScheduler(context: Context) {

    private val workManager =
        WorkManager.getInstance(context.applicationContext)

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun requestSync(ownerId: String) {
        require(ownerId.isNotBlank())

        val request = OneTimeWorkRequestBuilder<JournalSyncWorker>()
            .setInputData(
                workDataOf(JournalSyncWorker.OWNER_ID to ownerId)
            )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS,
            )
            .build()

        workManager.enqueueUniqueWork(
            immediateName(ownerId),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }

    fun ensurePeriodicSync(ownerId: String) {
        require(ownerId.isNotBlank())

        val request = PeriodicWorkRequestBuilder<JournalSyncWorker>(
            15,
            TimeUnit.MINUTES,
        )
            .setInputData(
                workDataOf(JournalSyncWorker.OWNER_ID to ownerId)
            )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS,
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            periodicName(ownerId),
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(ownerId: String) {
        workManager.cancelUniqueWork(immediateName(ownerId))
        workManager.cancelUniqueWork(periodicName(ownerId))
    }

    private fun immediateName(ownerId: String) =
        "journal-sync-$ownerId"

    private fun periodicName(ownerId: String) =
        "journal-sync-periodic-$ownerId"
}
