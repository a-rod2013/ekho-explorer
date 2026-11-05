package com.comp350sec001group2.ekhoexplorer.ui.pages.camera

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories.FeedRepository
import com.comp350sec001group2.ekhoexplorer.Business.Journal.JournalCaptureStoring
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoException
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoPreparer
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class CameraViewModel(
    private val repository: FeedRepository,
    private val preparer: PhotoPreparer,
    private val resolver: ContentResolver,
    private val journalStore: JournalCaptureStoring
) : ViewModel() {

    var photoPath by mutableStateOf<String?>(null)
        private set

    var pendingPath by mutableStateOf<String?>(null)
        private set

    var caption by mutableStateOf("")
        private set

    var isPosting by mutableStateOf(false)
        private set

    var posted by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var isSavingJournal by mutableStateOf(false)
        private set

    var journalSaved by mutableStateOf(false)
        private set

    var journalError by mutableStateOf<String?>(null)
        private set

    private var captureOwnerId: String? = null
    private var journalSession: JournalCaptureStoring.CaptureSession? = null
    private var journalSave: Deferred<Unit>? = null
    private var saveObserver: Job? = null

    fun updateCaption(value: String) {
        if (isPosting) {
            return
        }
        caption = value
        saveJournal()
    }

    fun prepareCapture(path: String): Boolean {
        if (isPosting || isSavingJournal) {
            return false
        }
        if (photoPath != null && !journalSaved) {
            journalError = "Save this photo to the journal before retaking."
            return false
        }
        val ownerId = journalStore.currentOwnerId()

        if (ownerId == null) {
            error = "Sign in before taking a journal photo."
            return false
        }

        if (photoPath != null && captureOwnerId != ownerId) {
            error = "The account changed. Close this preview first."
            return false
        }
        captureOwnerId = ownerId
        pendingPath = path
        error = null
        posted = false

        return true
    }

    fun finishCapture(success: Boolean) {
        val path = pendingPath
        pendingPath = null

        if (!success || path == null) {
            path?.let { File(it).delete() }
            return
        }

        // The preceding capture already has its own retained journal copy
        photoPath?.let { File(it).delete() }

        photoPath = path
        caption = ""
        journalSession = null
        journalSave = null
        journalSaved = false
        journalError = null

        saveJournal()
    }

    fun captureFailed() {
        finishCapture(false)
        error = "Could not open the camera."
    }

    fun retryJournalSave() {
        if (!isPosting && !isSavingJournal) {
            saveJournal()
        }
    }

    private fun saveJournal() {
        val path = photoPath ?: return
        val ownerId = captureOwnerId ?: return

        val session = try {
            journalSession ?: journalStore.startCapture(
                ownerId = ownerId,
                photoUri = Uri.fromFile(File(path)),
            ).also {
                journalSession = it
            }
        } catch (failure: Exception) {
            journalSaved = false
            journalError = failure.message ?: "Could not save the journal entry."
            return
        }

        val save = session.save(caption)
        journalSave = save

        isSavingJournal = true
        journalSaved = false
        journalError = null

        // Cancel only old UI observer, not app-scope save.
        saveObserver?.cancel()
        saveObserver = viewModelScope.launch {
            try {
                save.await()
                journalSaved = true
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                journalError = failure.message ?: "Could not save the journal entry."
            } finally {
                isSavingJournal = false
            }
        }
    }
    fun discardPhoto() {
        if (isPosting || isSavingJournal) {
            return
        }
        if (photoPath != null && !journalSaved) {
            journalError = "Retry saving before closing this photo."
            return
        }

        // Delete the camera original; journal retains its imported copy
        photoPath?.let { File(it).delete() }
        photoPath = null
        caption = ""
        error = null
        journalError = null
        journalSaved = false
        captureOwnerId = null
        journalSession = null
        journalSave = null
    }

    fun postPhoto(uri: Uri) {
        if (isPosting || posted || isSavingJournal) {
            return
        }

        val ownerId = captureOwnerId

        if (ownerId == null || journalStore.currentOwnerId() != ownerId) {
            error = "Sign in with the account that captured this photo."
            return
        }

        val savedDraft = journalSave

        if (!journalSaved || savedDraft == null) {
            journalError = "Save the journal entry before posting."
            return
        }

        val submittedCaption = caption.trim()

        isPosting = true
        error = null

        viewModelScope.launch {
            try {
                savedDraft.await()

                val sources = listOf(
                    {
                        resolver.openInputStream(uri)
                            ?: throw PhotoException(
                                PhotoErrorKind.INVALID_IMAGE,
                                "Could not open the photo"
                            )
                    }
                )

                val prepared = preparer.prepare(sources)

                check(journalStore.currentOwnerId() == ownerId) {
                    "The singed-in account changed before posting."
                }

                repository.addPost(
                    caption = submittedCaption,
                    imageUrls = emptyList(),
                    photos = prepared
                )

                posted = true
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (photoFailure: PhotoException) {
                error = when (photoFailure.kind) {
                    PhotoErrorKind.UNSUPPORTED_TYPE ->
                        "Only JPEG, PNG and WebP photos can be posted."

                    PhotoErrorKind.INVALID_IMAGE ->
                        "The photo could not be read."

                    PhotoErrorKind.INPUT_TOO_LARGE ->
                        "The photo is too large."

                    PhotoErrorKind.TOO_DETAILED ->
                        "The photo could not be compressed enough. Try another."

                    PhotoErrorKind.TOO_MANY_PHOTOS ->
                        "Too many photos were selected."
                }
            } catch (fireStoreFailure: FirestoreException) {
                error = "Could not post (${fireStoreFailure.kind}). Please check the feed before retrying."
            } catch (unexpectedFailure: Exception) {
                error = unexpectedFailure.localizedMessage ?: "Could not post the photo."
            } finally {
                isPosting = false
            }
        }
    }
}

val cameraViewModelFactory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as EkhoApp

        CameraViewModel(
            repository = app.container.feedRepository,
            preparer = app.container.photoPreparer,
            resolver = app.contentResolver,
            journalStore = app.container.journalCaptureStoring,
        )
    }
}
