package com.comp350sec001group2.ekhoexplorer.Business.Journal

import android.net.Uri
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class JournalCaptureStoring(
    private val repository: JournalRepository,
    private val photoStorage: JournalPhotoStorage,
    private val scope: CoroutineScope,
    private val ownerProvider: () -> String?,
) {
    fun currentOwnerId(): String? = ownerProvider()

    fun startCapture(
        ownerId: String,
        photoUri: Uri,
    ): CaptureSession {
        check(ownerId.isNotBlank() && currentOwnerId() == ownerId) {
            "The signed-in account changed. Capture a new photo."
        }

        return CaptureSession(ownerId, photoUri)
    }

    inner class CaptureSession internal constructor(
        private val ownerId: String,
        private val photoUri: Uri,
    ) {
        private val draftId = UUID.randomUUID().toString()
        private val createdAtMillis = System.currentTimeMillis()

        private val lock = Any()
        private var lastRequest = 0L
        private var pendingSave: Deferred<Unit>? = null

        // Accessed only by serialized save operations.
        private var retainedPhotoPath: String? = null
        private var savedNotes: String? = null
        private var lastUpdatedAtMillis = createdAtMillis

        fun save(notes: String): Deferred<Unit> =
            synchronized(lock) {
                val previousSave = pendingSave
                val request = ++lastRequest

                scope.async(Dispatchers.IO) {
                    // A later save waits for the preceding operation.
                    // join() also permits retrying after a failed save.
                    previousSave?.join()

                    val superseded = synchronized(lock) {
                        request != lastRequest
                    }

                    if (superseded) {
                        return@async
                    }

                    requireCurrentOwner()

                    if (savedNotes == notes) {
                        return@async
                    }

                    val photoPath = retainedPhotoPath
                        ?: photoStorage.importPhoto(ownerId, photoUri)
                            .also { retainedPhotoPath = it }

                    requireCurrentOwner()

                    val updatedAtMillis = maxOf(
                        System.currentTimeMillis(),
                        lastUpdatedAtMillis,
                        createdAtMillis,
                    )

                    repository.saveDraft(
                        JournalDraft(
                            id = draftId,
                            ownerId = ownerId,
                            title = "Observation",
                            notes = notes,
                            photoPaths = listOf(photoPath),
                            createdAtMillis = createdAtMillis,
                            updatedAtMillis = updatedAtMillis,
                        )
                    )

                    savedNotes = notes
                    lastUpdatedAtMillis = updatedAtMillis
                }.also {
                    pendingSave = it
                }
            }

        private fun requireCurrentOwner() {
            check(currentOwnerId() == ownerId) {
                "The signed-in account changed. Capture a new photo."
            }
        }
    }
}
