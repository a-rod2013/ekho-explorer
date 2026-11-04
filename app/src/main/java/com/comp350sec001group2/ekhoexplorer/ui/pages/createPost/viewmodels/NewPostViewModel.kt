package com.comp350sec001group2.ekhoexplorer.ui.pages.createPost.viewmodels

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.Repositories.FeedRepository
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoException
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoLimits
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoPreparer
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State for creating feed post. */
data class NewPostUiState(
    val caption: String = "",
    val photos: List<Uri> = emptyList(),
    val isPosting: Boolean = false,
    val message: String? = null,
    val posted: Boolean = false,
) {
    val canPost: Boolean get() = !isPosting && (caption.isNotBlank() || photos.isNotEmpty())
}

class NewPostViewModel(
    private val feed: FeedRepository,
    private val preparer: PhotoPreparer,
    private val resolver: ContentResolver,
    private val maxPhotos: Int = PhotoLimits().maxPhotos,
) : ViewModel() {

    private val _state = MutableStateFlow(NewPostUiState())
    val state: StateFlow<NewPostUiState> = _state

    val photoLimit: Int get() = maxPhotos

    fun onCaptionChange(text: String) = _state.update { it.copy(caption = text, message = null) }

    fun onPhotosPicked(uris: List<Uri>) = _state.update {
        val merged = (it.photos + uris).distinct()
        it.copy(
            photos = merged.take(maxPhotos),
            message = if (merged.size > maxPhotos) "Only $maxPhotos photos fit in a post." else null,
        )
    }

    fun onRemovePhoto(uri: Uri) = _state.update { it.copy(photos = it.photos - uri, message = null) }

    fun onMessageShown() = _state.update { it.copy(message = null) }

    fun onPost() {
        val current = _state.value
        if (!current.canPost) return
        _state.update { it.copy(isPosting = true, message = null) }
        viewModelScope.launch {
            try {
                val sources = current.photos.map { uri ->
                    {
                        resolver.openInputStream(uri)
                            ?: throw PhotoException(
                                PhotoErrorKind.INVALID_IMAGE,
                                "Could not open the photo"
                            )
                    }
                }
                val prepared = preparer.prepare(sources)
                feed.addPost(current.caption, emptyList(), prepared)
                _state.update { it.copy(isPosting = false, posted = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: PhotoException) {
                _state.update { it.copy(isPosting = false, message = photoMessage(e.kind)) }
            } catch (e: FirestoreException) {
                _state.update { it.copy(isPosting = false, message = "Could not post (${e.kind}). Please try again.") }
            }
        }
    }

    private fun photoMessage(kind: PhotoErrorKind) = when (kind) {
        PhotoErrorKind.UNSUPPORTED_TYPE -> "Only JPEG, PNG and WebP photos can be posted."
        PhotoErrorKind.INVALID_IMAGE -> "One of the photos could not be read."
        PhotoErrorKind.INPUT_TOO_LARGE -> "One of the photos is too large."
        PhotoErrorKind.TOO_DETAILED -> "One of the photos is too detailed to shrink enough. Try another."
        PhotoErrorKind.TOO_MANY_PHOTOS -> "Only $maxPhotos photos fit in a post."
    }
}

val newPostViewModelFactory = viewModelFactory {
    initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as EkhoApp
        NewPostViewModel(
            app.container.feedRepository,
            app.container.photoPreparer,
            app.contentResolver
        )
    }
}
