package com.comp350sec001group2.ekhoexplorer.ui.pages.camera

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
import com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories.FeedRepository
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

class CameraViewModel(
    private val repository: FeedRepository
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
        if (!posted) {
            photoPath?.let { File(it).delete() }
        }

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
        val submittedUri = uri.toString()

        isPosting = true
        error = null

        viewModelScope.launch {
            try {
                repository.addPost(
                    submittedCaption,
                    listOf(submittedUri)
                )

                posted = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Could not create the local post."
            } finally {
                isPosting = false
            }
        }
    }
}

val cameraViewModelFactory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as EkhoApp
        CameraViewModel(app.container.feedRepository)
    }
}
