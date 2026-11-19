package com.comp350sec001group2.ekhoexplorer.ui.pages.trailDetail.viewmodels

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Database.FirestoreException
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailReview
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoErrorKind
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoException
import com.comp350sec001group2.ekhoexplorer.Business.Photos.PhotoPreparer
import com.comp350sec001group2.ekhoexplorer.Business.Trails.Trail
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailLimits
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailPhotoLoader
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailRepository
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailRules
import com.comp350sec001group2.ekhoexplorer.Business.Trails.Viewer
import com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard.trailMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface GalleryItem {
    data class Owner(val index: Int) : GalleryItem
    data class FromReview(val reviewId: String, val index: Int) : GalleryItem
}

data class TrailDetailUiState(
    val trail: Trail? = null,
    val viewerId: String = "",
    val isAdmin: Boolean = false,
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val reviews: List<TrailReview> = emptyList(),
    val myReview: TrailReview? = null,
    val myHearts: Set<String> = emptySet(),
    val reviewRating: Int = 0,
    val reviewText: String = "",
    val reviewPhotos: List<Uri> = emptyList(),
    val editingReview: Boolean = false,
    val isPosting: Boolean = false,
    val message: String? = null,
    val deleted: Boolean = false,
) {
    val isOwner: Boolean get() = viewerId.isNotEmpty() && trail?.ownerId == viewerId

    val canEdit: Boolean get() = trail != null && TrailRules.canEdit(trail, Viewer(viewerId, isAdmin, emptySet()))

    val canRequestVerification: Boolean get() = trail != null && TrailRules.canRequestVerification(trail, Viewer(viewerId, isAdmin, emptySet()))

    val canPromote: Boolean get() = isOwner

    val showReviewForm: Boolean get() = viewerId.isNotEmpty() && (myReview == null || editingReview)

    fun canDelete(review: TrailReview): Boolean =
        viewerId.isNotEmpty() && (review.userId == viewerId || isOwner || isAdmin)

    fun isPromoted(reviewId: String, index: Int): Boolean = trail?.galleryPhotos?.contains("$reviewId/$index") == true

    val gallery: List<GalleryItem>
        get() {
            val t = trail ?: return emptyList()
            val own = (0 until t.photoCount).map { GalleryItem.Owner(it) }
            val promoted = t.galleryPhotos.mapNotNull { entry ->
                val at = entry.lastIndexOf('/')
                val index = entry.substring(at + 1).toIntOrNull()
                if (at <= 0 || index == null) null else GalleryItem.FromReview(entry.substring(0, at), index)
            }
            return own + promoted
        }
}

class TrailDetailViewModel(
    private val trailId: String,
    private val repository: TrailRepository,
    private val preparer: PhotoPreparer,
    private val resolver: ContentResolver,
) : ViewModel() {

    val photos: TrailPhotoLoader get() = repository

    private data class Local(
        val loaded: Trail? = null,
        val isLoading: Boolean = true,
        val notFound: Boolean = false,
        val reviews: List<TrailReview> = emptyList(),
        val myReview: TrailReview? = null,
        val myHearts: Set<String> = emptySet(),
        val reviewRating: Int = 0,
        val reviewText: String = "",
        val reviewPhotos: List<Uri> = emptyList(),
        val editingReview: Boolean = false,
        val isPosting: Boolean = false,
        val message: String? = null,
        val deleted: Boolean = false,
    )

    private val local = MutableStateFlow(Local())

    val state: StateFlow<TrailDetailUiState> =
        combine(repository.trails, local, EhkoCache.activeUserFlow) { all, l, user ->
            val mine = l.myReview
            val everyone = if (mine != null && l.reviews.none { it.id == mine.id }) l.reviews + mine else l.reviews
            TrailDetailUiState(
                trail = all.firstOrNull { it.id == trailId } ?: l.loaded.takeUnless { l.deleted },
                viewerId = user.userId,
                isAdmin = user.isAdmin,
                isLoading = l.isLoading,
                notFound = l.notFound,
                reviews = everyone.sortedWith(
                    compareByDescending<TrailReview> { it.heartCount }.thenByDescending { it.createdAtMillis }.thenBy { it.id },
                ),
                myReview = mine,
                myHearts = l.myHearts,
                reviewRating = l.reviewRating,
                reviewText = l.reviewText,
                reviewPhotos = l.reviewPhotos,
                editingReview = l.editingReview,
                isPosting = l.isPosting,
                message = l.message,
                deleted = l.deleted,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrailDetailUiState())

    init {
        viewModelScope.launch { load() }
    }

    fun refresh() {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        try {
            val trail = repository.get(trailId)
            if (trail == null) {
                local.update { it.copy(notFound = true, isLoading = false) }
                return
            }
            local.update { it.copy(loaded = trail, notFound = false) }
            loadReviews()
            local.update { it.copy(isLoading = false) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirestoreException) {
            local.update { it.copy(isLoading = false, message = "Could not load this trail (${e.kind}). Pull down or reopen to try again.") }
        }
    }

    private suspend fun loadReviews() {
        val mine = repository.viewerState(trailId)
        val page = repository.reviews(trailId, null)
        local.update { it.copy(reviews = page.items, myReview = mine.myReview, myHearts = mine.myHearts) }
    }


    fun onToggleHeart(reviewId: String) {
        viewModelScope.launch {
            guarded {
                val now = repository.toggleHeart(trailId, reviewId)
                local.update { l ->
                    val bump = if (now) 1 else -1
                    l.copy(
                        myHearts = if (now) l.myHearts + reviewId else l.myHearts - reviewId,
                        reviews = l.reviews.map { if (it.id == reviewId) it.copy(heartCount = (it.heartCount + bump).coerceAtLeast(0)) else it },
                        myReview = l.myReview?.let { if (it.id == reviewId) it.copy(heartCount = (it.heartCount + bump).coerceAtLeast(0)) else it },
                    )
                }
            }
        }
    }


    fun onReviewRatingChange(stars: Int) {
        if (stars in 1..5) local.update { it.copy(reviewRating = stars) }
    }

    fun onReviewTextChange(text: String) = local.update { it.copy(reviewText = text.take(TrailLimits.REVIEW_CAPTION_MAX)) }

    fun onReviewPhotosPicked(uris: List<Uri>) = local.update {
        val merged = (it.reviewPhotos + uris).distinct()
        it.copy(
            reviewPhotos = merged.take(TrailLimits.REVIEW_PHOTOS),
            message = if (merged.size > TrailLimits.REVIEW_PHOTOS) "A review can have at most ${TrailLimits.REVIEW_PHOTOS} pictures." else it.message,
        )
    }

    fun onRemoveReviewPhoto(uri: Uri) = local.update { it.copy(reviewPhotos = it.reviewPhotos - uri) }

    fun onEditReview() {
        val mine = local.value.myReview ?: return
        local.update { it.copy(editingReview = true, reviewRating = mine.rating, reviewText = mine.caption, reviewPhotos = emptyList()) }
    }

    fun onCancelEdit() = local.update { it.copy(editingReview = false, reviewRating = 0, reviewText = "", reviewPhotos = emptyList()) }

    fun onSubmitReview() {
        val current = local.value
        if (current.isPosting) return
        if (current.reviewRating !in 1..5) {
            local.update { it.copy(message = "Choose a star rating for your review.") }
            return
        }
        local.update { it.copy(isPosting = true, message = null) }
        viewModelScope.launch {
            try {
                // No new pictures: a first review has none, an edited one keeps its own.
                val prepared = if (current.reviewPhotos.isEmpty()) null else preparer.prepare(
                    current.reviewPhotos.map { uri ->
                        {
                            resolver.openInputStream(uri) ?: throw PhotoException(PhotoErrorKind.INVALID_IMAGE, "Could not open the picture")
                        }
                    },
                )
                repository.saveReview(trailId, current.reviewRating, current.reviewText, prepared)
                loadReviews()
                local.update { it.copy(editingReview = false, reviewRating = 0, reviewText = "", reviewPhotos = emptyList(), isPosting = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: PhotoException) {
                local.update { it.copy(isPosting = false, message = e.kind.trailMessage()) }
            } catch (e: FirestoreException) {
                local.update { it.copy(isPosting = false, message = "Could not save the review (${e.kind}). Please try again.") }
            }
        }
    }

    fun onDeleteReview(reviewId: String) {
        viewModelScope.launch {
            guarded {
                repository.deleteReview(trailId, reviewId)
                loadReviews()
                local.update { if (it.myReview == null) it.copy(editingReview = false) else it }
            }
        }
    }

    fun onSetPromoted(reviewId: String, index: Int, promote: Boolean) {
        val current = state.value
        if (promote && !current.isPromoted(reviewId, index) && (current.trail?.galleryPhotos?.size ?: 0) >= TrailLimits.PROMOTED_PHOTOS) {
            local.update { it.copy(message = "The gallery holds at most ${TrailLimits.PROMOTED_PHOTOS} pictures from reviews. Remove one first.") }
            return
        }
        viewModelScope.launch { guarded { repository.setPromoted(trailId, reviewId, index, promote) } }
    }

    fun onRequestVerification() {
        viewModelScope.launch { guarded { repository.requestVerification(trailId) } }
    }

    fun onDeleteTrail() {
        viewModelScope.launch { guarded { repository.delete(trailId); local.update { it.copy(deleted = true) } } }
    }

    fun onMessageShown() = local.update { it.copy(message = null) }

    private suspend fun guarded(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirestoreException) {
            local.update { it.copy(message = "That did not work (${e.kind}). Please try again.") }
        }
    }
}
