package com.comp350sec001group2.ekhoexplorer.ui.pages.trailDetail.views

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.TrailReview
import com.comp350sec001group2.ekhoexplorer.Business.Trails.LatLngPoint
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailFormat
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailGeo
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailLimits
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailPhotoLoader
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailStatus
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailVisibility
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import com.comp350sec001group2.ekhoexplorer.ui.components.LocalTrailPhotoLoader
import com.comp350sec001group2.ekhoexplorer.ui.components.StoredImage
import com.comp350sec001group2.ekhoexplorer.ui.components.HeartButton
import com.comp350sec001group2.ekhoexplorer.ui.components.label
import com.comp350sec001group2.ekhoexplorer.ui.pages.trailDetail.viewmodels.GalleryItem
import com.comp350sec001group2.ekhoexplorer.ui.pages.trailDetail.viewmodels.TrailDetailUiState
import com.comp350sec001group2.ekhoexplorer.ui.pages.trailDetail.viewmodels.TrailDetailViewModel
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import java.nio.ByteBuffer
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

fun trailDetailViewModelFactory(trailId: String) = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as EkhoApp
        TrailDetailViewModel(trailId, app.container.trailRepository, app.container.photoPreparer, app.contentResolver)
    }
}

@Composable
fun TrailDetailScreen(
    trailId: String,
    onEdit: (String) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrailDetailViewModel = viewModel(key = "trail-$trailId", factory = trailDetailViewModelFactory(trailId)),
    pathMap: @Composable (List<LatLngPoint>) -> Unit = { TrailPathView(it) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val d = AppStyle.dimens

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }

    CompositionLocalProvider(LocalTrailPhotoLoader provides viewModel.photos) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = AppStyle.colors.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            val trail = state.trail
            when {
                trail == null && state.isLoading -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                trail == null -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (state.notFound) "This trail is not available." else "Could not load this trail.", color = AppStyle.colors.textSecondary)
                }
                else -> TrailContent(state, viewModel, pathMap, onEdit, Modifier.padding(padding))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrailContent(
    state: TrailDetailUiState,
    viewModel: TrailDetailViewModel,
    pathMap: @Composable (List<LatLngPoint>) -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier,
) {
    val trail = state.trail ?: return
    val d = AppStyle.dimens
    val path = remember(trail.pathEncoded) { TrailGeo.decode(trail.pathEncoded) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var reviewsOpen by rememberSaveable { mutableStateOf(false) }

    val hasMap = path.size >= 2
    val hasOwnerActions = state.canEdit || state.canRequestVerification ||
        (state.isOwner && trail.verificationRequested && trail.status == TrailStatus.UNVERIFIED)
    val hasDescription = trail.description.isNotBlank()
    val hasGallery = state.gallery.isNotEmpty()
    val reviewsIndex = 1 + (if (hasMap) 1 else 0) + 1 + (if (hasOwnerActions) 1 else 0) +
        (if (hasDescription) 1 else 0) + (if (hasGallery) 1 else 0)

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().testTag("trail-content"),
        verticalArrangement = Arrangement.spacedBy(d.md),
    ) {
        // Title and badges, location, then length, difficulty, rating and visibility.
        item {
            Column(Modifier.padding(horizontal = d.lg, vertical = d.sm), verticalArrangement = Arrangement.spacedBy(d.xs)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.sm)) {
                    Text(trail.name, style = AppStyle.type.username, color = AppStyle.colors.textPrimary, modifier = Modifier.weight(1f, fill = false).testTag("trail-title"))
                    if (trail.status == TrailStatus.VERIFIED) {
                        Icon(Icons.Filled.Verified, "Verified trail", tint = AppStyle.colors.accent, modifier = Modifier.size(d.icon).testTag("verified-badge-detail"))
                    } else if (trail.visibility == TrailVisibility.PUBLIC) {
                        Icon(Icons.Outlined.Verified, "Unverified trail", tint = AppStyle.colors.textSecondary, modifier = Modifier.size(d.icon).testTag("unverified-badge-detail"))
                    }
                }
                val where = listOf(trail.location, "by ${trail.ownerName}").filter { it.isNotBlank() }.joinToString("  -  ")
                Text(where, style = AppStyle.type.caption, color = AppStyle.colors.textSecondary, modifier = Modifier.testTag("trail-location"))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(d.md), verticalArrangement = Arrangement.spacedBy(d.xs)) {
                    Text(TrailFormat.miles(trail.lengthMeters), style = AppStyle.type.caption, color = AppStyle.colors.textPrimary, modifier = Modifier.testTag("detail-length"))
                    Text("Difficulty ${trail.difficulty} of 5", style = AppStyle.type.caption, color = AppStyle.colors.textPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.xs)) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = AppStyle.colors.accent, modifier = Modifier.size(16.dp))
                        Text(TrailFormat.ratingText(trail), style = AppStyle.type.caption, color = AppStyle.colors.textPrimary, modifier = Modifier.testTag("detail-rating"))
                    }
                    Text(trail.visibility.label(), style = AppStyle.type.caption, color = AppStyle.colors.textPrimary, modifier = Modifier.testTag("detail-visibility"))
                }
            }
        }

        if (hasMap) {
            item {
                Column {
                    Box(Modifier.fillMaxWidth().height(220.dp)) { pathMap(path) }
                    HorizontalDivider(color = AppStyle.colors.divider)
                }
            }
        }

        // The action bar: jump to the reviews, write (or change) my review.
        item {
            Column {
                Row(Modifier.fillMaxWidth().padding(horizontal = d.sm), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    ActionBarButton(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        description = "Go to reviews",
                        label = "Reviews (${trail.ratingCount})",
                        tint = AppStyle.colors.textSecondary,
                        tag = "jump-to-reviews",
                        onClick = {
                            reviewsOpen = true
                            scope.launch { listState.animateScrollToItem(reviewsIndex) }
                        },
                    )
                    ActionBarButton(
                        icon = if (state.myReview != null) Icons.Filled.Star else Icons.Filled.StarBorder,
                        description = if (state.myReview != null) "Edit your review" else "Write a review",
                        label = if (state.myReview != null) "Edit review" else "Write a review",
                        tint = if (state.myReview != null) AppStyle.colors.accent else AppStyle.colors.textSecondary,
                        tag = "write-review",
                        onClick = {
                            if (state.myReview != null) viewModel.onEditReview()
                            reviewsOpen = true
                            scope.launch { listState.animateScrollToItem(reviewsIndex) }
                        },
                    )
                }
                HorizontalDivider(color = AppStyle.colors.divider)
            }
        }

        if (hasOwnerActions) {
            item { OwnerActions(state, viewModel, onEdit = { onEdit(trail.id) }) }
        }

        if (hasDescription) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(d.md)) {
                    Text(trail.description, color = AppStyle.colors.textPrimary, modifier = Modifier.padding(horizontal = d.lg).testTag("trail-description"))
                    HorizontalDivider(color = AppStyle.colors.divider)
                }
            }
        }

        if (hasGallery) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(d.sm)) {
                    Text("Gallery", style = AppStyle.type.username, color = AppStyle.colors.textPrimary, modifier = Modifier.padding(horizontal = d.lg).testTag("gallery-title"))
                    Gallery(trail.id, state.gallery)
                    HorizontalDivider(color = AppStyle.colors.divider)
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { reviewsOpen = !reviewsOpen },
                modifier = Modifier.fillMaxWidth().padding(horizontal = d.lg).testTag("view-reviews"),
            ) { Text(if (reviewsOpen) "Hide reviews" else "View reviews (${trail.ratingCount})") }
        }

        if (reviewsOpen) {
            item {
                Text("Reviews (${trail.ratingCount})", style = AppStyle.type.username, color = AppStyle.colors.textPrimary, modifier = Modifier.padding(horizontal = d.lg).testTag("reviews-title"))
            }
            if (state.showReviewForm) {
                item { ReviewForm(state, viewModel) }
            }
            if (state.reviews.isEmpty()) {
                item { Text("No reviews yet. Be the first.", style = AppStyle.type.caption, color = AppStyle.colors.textSecondary, modifier = Modifier.padding(horizontal = d.lg)) }
            }
            items(state.reviews, key = { it.id }) { review ->
                ReviewRow(trail.id, review, state, viewModel)
            }
        }
    }
}

@Composable
private fun ActionBarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    tag: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, modifier = Modifier.testTag(tag)) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(AppStyle.dimens.icon))
        Text(label, style = AppStyle.type.count, color = tint, modifier = Modifier.padding(start = AppStyle.dimens.xs))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OwnerActions(state: TrailDetailUiState, viewModel: TrailDetailViewModel, onEdit: () -> Unit) {
    val d = AppStyle.dimens
    val trail = state.trail ?: return
    var confirmDelete by remember { mutableStateOf(false) }

    Column {
        Column(Modifier.padding(horizontal = d.lg, vertical = d.xs), verticalArrangement = Arrangement.spacedBy(d.sm)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(d.sm), verticalArrangement = Arrangement.spacedBy(d.sm)) {
                if (state.canEdit) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.testTag("edit-trail")) { Text("Edit") }
                    OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.testTag("delete-trail")) { Text("Delete") }
                }
                if (state.canRequestVerification) {
                    OutlinedButton(onClick = viewModel::onRequestVerification, modifier = Modifier.testTag("request-verification-detail")) { Text("Request review") }
                }
            }
            if (!state.canRequestVerification && state.isOwner && trail.verificationRequested && trail.status == TrailStatus.UNVERIFIED) {
                Text("Verification requested", style = AppStyle.type.caption, color = AppStyle.colors.accent, modifier = Modifier.testTag("verification-requested-detail"))
            }
        }
        HorizontalDivider(color = AppStyle.colors.divider)
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this trail?") },
            text = { Text("The trail, its pictures and its reviews will be removed for everyone. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.onDeleteTrail() }, modifier = Modifier.testTag("confirm-delete")) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }, modifier = Modifier.testTag("cancel-delete")) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StarRow(selected: Int, onRate: (Int) -> Unit) {
    Row {
        for (star in 1..5) {
            IconButton(onClick = { onRate(star) }, modifier = Modifier.testTag("star-$star")) {
                Icon(
                    imageVector = if (star <= selected) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "Rate $star of 5",
                    tint = AppStyle.colors.accent,
                )
            }
        }
    }
}

@Composable
private fun Gallery(trailId: String, items: List<GalleryItem>) {
    val d = AppStyle.dimens
    val loader = LocalTrailPhotoLoader.current
    LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = d.lg), horizontalArrangement = Arrangement.spacedBy(d.sm)) {
        items(items, key = { it.toString() }) { item ->
            when (item) {
                is GalleryItem.Owner -> GalleryImage(
                    cacheKey = "trail:$trailId:${item.index}",
                    tag = "gallery-item-owner-${item.index}",
                    loader = { loader.loadTrailPhoto(trailId, item.index) },
                )
                is GalleryItem.FromReview -> GalleryImage(
                    cacheKey = "tc:$trailId:${item.reviewId}:${item.index}",
                    tag = "gallery-item-${item.reviewId}-${item.index}",
                    loader = { loader.loadReviewPhoto(trailId, item.reviewId, item.index) },
                )
            }
        }
    }
}

private sealed interface GalleryState {
    data object Loading : GalleryState
    class Loaded(val bytes: ByteArray) : GalleryState
    data object Missing : GalleryState
    data object Failed : GalleryState
}

@Composable
private fun GalleryImage(cacheKey: String, tag: String, loader: suspend () -> ByteArray?) {
    var attempt by remember(cacheKey) { mutableIntStateOf(0) }
    val state by produceState<GalleryState>(GalleryState.Loading, cacheKey, attempt) {
        value = GalleryState.Loading
        value = try {
            loader()?.let { GalleryState.Loaded(it) } ?: GalleryState.Missing
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            GalleryState.Failed
        }
    }
    val size = Modifier.size(width = 240.dp, height = 180.dp).clip(RoundedCornerShape(AppStyle.dimens.radiusSm))
    when (val s = state) {
        GalleryState.Missing -> Unit
        GalleryState.Loading -> Box(size.background(AppStyle.colors.surface), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }
        GalleryState.Failed -> TextButton(onClick = { attempt++ }, modifier = size.background(AppStyle.colors.surface)) { Text("Picture unavailable. Tap to retry.") }
        is GalleryState.Loaded -> {
            val context = LocalContext.current
            val request = remember(s.bytes) { ImageRequest.Builder(context).data(ByteBuffer.wrap(s.bytes)).memoryCacheKey(cacheKey).build() }
            AsyncImage(model = request, contentDescription = "Trail picture", contentScale = ContentScale.Crop, modifier = size.testTag(tag))
        }
    }
}

@Composable
private fun StarsShown(rating: Int) {
    Row {
        for (star in 1..5) {
            Icon(
                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = AppStyle.colors.accent,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ReviewRow(trailId: String, review: TrailReview, state: TrailDetailUiState, viewModel: TrailDetailViewModel) {
    val d = AppStyle.dimens
    val loader = LocalTrailPhotoLoader.current
    val mine = review.userId == state.viewerId && state.viewerId.isNotEmpty()
    Column(Modifier.fillMaxWidth().padding(horizontal = d.lg).testTag("review-${review.id}"), verticalArrangement = Arrangement.spacedBy(d.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (mine) "${review.userName} (you)" else review.userName,
                style = AppStyle.type.username,
                color = AppStyle.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (mine) {
                TextButton(onClick = viewModel::onEditReview, modifier = Modifier.testTag("edit-review")) { Text("Edit") }
            }
            if (state.canDelete(review)) {
                TextButton(onClick = { viewModel.onDeleteReview(review.id) }, modifier = Modifier.testTag("delete-review-${review.id}")) { Text("Delete") }
            }
        }
        Box(Modifier.testTag("review-stars-${review.id}")) { StarsShown(review.rating) }
        if (review.caption.isNotBlank()) {
            Text(review.caption, color = AppStyle.colors.textPrimary, modifier = Modifier.testTag("review-text-${review.id}"))
        }
        if (review.photoCount > 0) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
                items((0 until review.photoCount).toList()) { index ->
                    Column(verticalArrangement = Arrangement.spacedBy(d.xs)) {
                        StoredImage(
                            cacheKey = "tr:$trailId:${review.id}:$index",
                            tag = "review-photo-${review.id}-$index",
                            modifier = Modifier.size(140.dp).clip(RoundedCornerShape(d.radiusSm)),
                            load = { loader.loadReviewPhoto(trailId, review.id, index) },
                        )
                        if (state.canPromote) {
                            val promoted = state.isPromoted(review.id, index)
                            OutlinedButton(
                                onClick = { viewModel.onSetPromoted(review.id, index, !promoted) },
                                modifier = Modifier.width(140.dp).testTag(if (promoted) "unpromote-${review.id}-$index" else "promote-${review.id}-$index"),
                            ) { Text(if (promoted) "Remove from gallery" else "Add to gallery", textAlign = TextAlign.Center) }
                        }
                    }
                }
            }
        }
        HeartButton(
            count = review.heartCount,
            hearted = review.id in state.myHearts,
            onClick = { viewModel.onToggleHeart(review.id) },
            modifier = Modifier.testTag("heart-${review.id}"),
        )
        HorizontalDivider(color = AppStyle.colors.divider)
    }
}

@Composable
private fun ReviewForm(state: TrailDetailUiState, viewModel: TrailDetailViewModel) {
    val d = AppStyle.dimens
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(TrailLimits.REVIEW_PHOTOS)) {
        viewModel.onReviewPhotosPicked(it)
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = d.lg, vertical = d.sm).testTag("review-form"), verticalArrangement = Arrangement.spacedBy(d.sm)) {
        Text(if (state.editingReview) "Change your review" else "Write your review", style = AppStyle.type.username, color = AppStyle.colors.textPrimary)
        StarRow(selected = state.reviewRating, onRate = viewModel::onReviewRatingChange)
        OutlinedTextField(
            value = state.reviewText,
            onValueChange = viewModel::onReviewTextChange,
            label = { Text("Caption (optional)") },
            enabled = !state.isPosting,
            minLines = 2,
            supportingText = { Text("${TrailLimits.REVIEW_CAPTION_MAX - state.reviewText.length} left", modifier = Modifier.testTag("review-counter")) },
            modifier = Modifier.fillMaxWidth().testTag("review-input"),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
            items(state.reviewPhotos, key = { it.toString() }) { uri ->
                Box(Modifier.size(72.dp).clip(RoundedCornerShape(d.radiusSm))) {
                    AsyncImage(model = uri, contentDescription = "Chosen picture", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    IconButton(onClick = { viewModel.onRemoveReviewPhoto(uri) }, modifier = Modifier.align(Alignment.TopEnd).size(28.dp)) {
                        Icon(Icons.Filled.Close, "Remove picture", tint = AppStyle.colors.onAccent)
                    }
                }
            }
        }
        if (state.editingReview && state.myReview?.photoCount?.let { it > 0 } == true && state.reviewPhotos.isEmpty()) {
            Text("Your pictures stay as they are unless you pick new ones, which replace them.", style = AppStyle.type.caption, color = AppStyle.colors.textSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
            OutlinedButton(
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                enabled = !state.isPosting && state.reviewPhotos.size < TrailLimits.REVIEW_PHOTOS,
                modifier = Modifier.testTag("review-pictures"),
            ) { Text("Pictures (${state.reviewPhotos.size} of ${TrailLimits.REVIEW_PHOTOS})") }
            Button(onClick = viewModel::onSubmitReview, enabled = !state.isPosting, modifier = Modifier.testTag("submit-review")) {
                if (state.isPosting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(if (state.editingReview) "Save changes" else "Post review")
            }
            if (state.editingReview) {
                TextButton(onClick = viewModel::onCancelEdit, enabled = !state.isPosting, modifier = Modifier.testTag("cancel-review-edit")) { Text("Cancel") }
            }
        }
    }
}
