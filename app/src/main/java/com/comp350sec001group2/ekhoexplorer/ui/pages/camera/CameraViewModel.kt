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
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories.FeedRepository
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoException
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoPreparer
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File

class CameraViewModel(
    private val repository: FeedRepository,
    private val preparer: PhotoPreparer,
    private val resolver: ContentResolver
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

    fun updateCaption(value: String) {
        caption = value
    }

    fun prepareCapture(path: String) {
        pendingPath = path
        error = null
    }

    fun finishCapture(success: Boolean) {
        val path = pendingPath
        pendingPath = null

        if (success && path != null) {
            photoPath?.let { File(it).delete() }
            photoPath = path
        } else {
            path?.let { File(it).delete() }
        }
    }

    fun captureFailed() {
        finishCapture(false)
        error = "Could not open the camera."
    }

    fun discardPhoto() {
        // The feed now loads the saved photo from the database.
        photoPath?.let { File(it).delete() }
        photoPath = null
        caption = ""
        error = null
    }

    fun postPhoto(uri: Uri) {
        if (isPosting || posted) return

        if (EhkoCache.activeUser.userId.isBlank()) {
            error = "Sign in before posting."
            return
        }

        val submittedCaption = caption.trim()

        isPosting = true
        error = null

        viewModelScope.launch {
            try {
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

                repository.addPost(
                    caption = submittedCaption,
                    imageUrls = emptyList(),
                    photos = prepared
                )

                posted = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: PhotoException) {
                error = when (e.kind) {
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
            } catch (e: FirestoreException) {
                error = "Could not post (${e.kind}). Please check the feed before retrying."
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Could not post the photo."
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
            resolver = app.contentResolver
        )
    }
}
