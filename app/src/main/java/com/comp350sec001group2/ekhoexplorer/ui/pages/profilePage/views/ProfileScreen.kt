
package com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.views

import com.comp350sec001group2.ekhoexplorer.ui.components.AccountMenu
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Post
import com.comp350sec001group2.ekhoexplorer.ui.components.CommentsSheet
import com.comp350sec001group2.ekhoexplorer.ui.components.LocalPhotoLoader
import com.comp350sec001group2.ekhoexplorer.ui.components.PostCard
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels.ProfileUiState
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels.ProfileViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels.profileViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import java.nio.ByteBuffer
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(
        factory = profileViewModelFactory
    )
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current

    var selectedPost by remember {
        mutableStateOf<Post?>(null)
    }

    var commentsPostId by remember {
        mutableStateOf<String?>(null)
    }

    // Opens the Android image picker for avatar selection.
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadAvatar(
                uri,
                context.contentResolver
            )
        }
    }

    // Clears avatar upload messages after two seconds.
    LaunchedEffect(
        uiState.avatarUploadMessage,
        uiState.avatarErrorMessage
    ) {
        if (
            uiState.avatarUploadMessage != null ||
            uiState.avatarErrorMessage != null
        ) {
            delay(2000L)
            viewModel.clearAvatarMessage()
        }
    }

    // Provides the shared photo loader to thumbnails and post popups.
    CompositionLocalProvider(
        LocalPhotoLoader provides viewModel.photos
    ) {
        ProfileScreenContent(
            uiState = uiState,
            onAvatarClick = {
                if (
                    !uiState.isUploadingAvatar &&
                    !uiState.isLoading
                ) {
                    avatarPicker.launch("image/*")
                }
            },
            onEditBio = viewModel::startEditingBio,
            onBioDraftChange = viewModel::updateBioDraft,
            onSaveBio = viewModel::saveBio,
            onCancelBio = viewModel::cancelEditingBio,
            onPostClick = { post ->
                selectedPost = post
            },
            onSignOut = onSignOut
        )

        // Displays the selected post in a dialog.
        selectedPost?.let { selected ->
            val post = uiState.posts.firstOrNull {
                it.id == selected.id
            } ?: selected

            Dialog(
                onDismissRequest = {
                    selectedPost = null
                }
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AppStyle.colors.background
                ) {
                    PostCard(
                        post = post,
                        onVote = { vote ->
                            viewModel.onPostVote(post.id, vote)
                        },
                        isOwner = true,
                        onOpenComments = {
                            selectedPost = null
                            commentsPostId = post.id
                        },
                        onDelete = {
                            viewModel.onDeletePost(post.id) {
                                selectedPost = null
                            }
                        },
                        onHideUser = {},
                        onBlockUser = {}
                    )
                }
            }
        }

        // Opens comments separately from the post dialog.
        commentsPostId?.let { postId ->
            CommentsSheet(
                postId = postId,
                onDismiss = {
                    commentsPostId = null
                    viewModel.refreshUserPosts()
                }
            )
        }
    }
}

@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    onPostClick: (Post) -> Unit,
    onEditBio: () -> Unit,
    onBioDraftChange: (String) -> Unit,
    onSaveBio: () -> Unit,
    onCancelBio: () -> Unit,
    onAvatarClick: () -> Unit = {},
    onSignOut: (() -> Unit)? = null
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppStyle.colors.background
    ) { innerPadding ->

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                bottom = AppStyle.dimens.lg
            )
        ) {
            // Profile header.
            item(span = { GridItemSpan(3) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppStyle.dimens.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Displays the stored avatar or default silhouette.
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .clickable(
                                enabled = !uiState.isUploadingAvatar &&
                                    !uiState.isLoading,
                                onClick = onAvatarClick
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarBitmap = uiState.avatarBitmap

                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap.asImageBitmap(),
                                contentDescription = "Profile picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Default profile picture",
                                modifier = Modifier.fillMaxSize(),
                                tint = AppStyle.colors.textSecondary
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = AppStyle.dimens.md),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            color = AppStyle.colors.textPrimary
                        )

                        Text(
                            text = "@${uiState.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppStyle.colors.textSecondary
                        )

                        // Displays the account creation date.
                        if (uiState.joinDate.isNotBlank()) {
                            Text(
                                text = "Explorer since ${uiState.joinDate}",
                                modifier = Modifier.padding(
                                    top = AppStyle.dimens.sm
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppStyle.colors.textSecondary
                            )
                        }

                        if (uiState.isUploadingAvatar) {
                            Text(
                                text = "Uploading picture...",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppStyle.colors.textSecondary
                            )
                        }

                        uiState.avatarErrorMessage?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        uiState.avatarUploadMessage?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppStyle.colors.textSecondary
                            )
                        }
                    }
                    if (onSignOut != null) {
                        AccountMenu(
                            username = uiState.username,
                            onSignOut = onSignOut
                        )
                    }
                }
            }

            // Profile observation and species statistics.
            item(span = { GridItemSpan(3) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = AppStyle.dimens.lg
                        ),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileStat(
                        count = uiState.observations,
                        label = "Observations"
                    )

//                    ProfileStat(
//                        count = uiState.species,
//                        label = "Species"
//                    )
                }
            }

            // Editable profile bio.
            item(span = { GridItemSpan(3) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = AppStyle.dimens.lg
                        )
                        .padding(
                            vertical = AppStyle.dimens.xl
                        )
                ) {
                    if (uiState.isEditingBio) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BasicTextField(
                                value = uiState.bioDraft,
                                onValueChange = onBioDraftChange,
                                enabled = !uiState.isSavingBio,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = AppStyle.colors.textPrimary
                                ),
                                decorationBox = { innerTextField ->
                                    if (uiState.bioDraft.isEmpty()) {
                                        Text(
                                            text = "What do you want to write?",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AppStyle.colors.textSecondary
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            uiState.errorMessage?.let { message ->
                                Text(
                                    text = message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = onCancelBio,
                                    enabled = !uiState.isSavingBio
                                ) {
                                    Text("Cancel")
                                }

                                TextButton(
                                    onClick = onSaveBio,
                                    enabled = !uiState.isSavingBio &&
                                        uiState.bioDraft.length <= 160
                                ) {
                                    Text(
                                        if (uiState.isSavingBio) {
                                            "Saving..."
                                        } else {
                                            "Save Edits"
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Displays the saved bio or an appropriate placeholder.
                        Text(
                            text = when {
                                uiState.isLoading ->
                                    "Loading profile..."

                                uiState.errorMessage != null ->
                                    uiState.errorMessage

                                uiState.bio.isBlank() ->
                                    "No bio yet. Tap the edit icon to add one."

                                else -> uiState.bio
                            } ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppStyle.colors.textPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    end = AppStyle.dimens.xl
                                )
                        )

                        IconButton(
                            onClick = onEditBio,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(AppStyle.dimens.icon),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Transparent
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit bio",
                                tint = AppStyle.colors.textSecondary
                            )
                        }
                    }
                }
            }

            // Divider between profile information and posts.
            item(span = { GridItemSpan(3) }) {
                HorizontalDivider(
                    color = AppStyle.colors.divider
                )
            }

            // Displays posts belonging to the signed-in account.
            items(
                items = uiState.posts,
                key = { post -> post.id }
            ) { post ->
                ProfilePostThumbnail(
                    post = post,
                    onClick = {
                        onPostClick(post)
                    }
                )
            }

            if (uiState.isLoadingPosts) {
                item(span = { GridItemSpan(3) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AppStyle.colors.accent
                        )
                    }
                }
            }

            uiState.postsErrorMessage?.let { message ->
                item(span = { GridItemSpan(3) }) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// Represents the loading state of a post thumbnail.
private sealed interface ThumbnailState {
    data object Loading : ThumbnailState
    data class Loaded(val bytes: ByteArray) : ThumbnailState
    data object Unavailable : ThumbnailState
}

@Composable
private fun ProfilePostThumbnail(
    post: Post,
    onClick: () -> Unit
) {
    val photoLoader = LocalPhotoLoader.current
    val context = LocalContext.current

    // Retrieves the first photo belonging to the post.
    val photoState by produceState<ThumbnailState>(
        initialValue = ThumbnailState.Loading,
        post.id,
        post.photoCount,
        photoLoader
    ) {
        value = ThumbnailState.Loading

        value = if (post.photoCount > 0) {
            try {
                val bytes = photoLoader.load(post.id, 0)

                if (bytes != null) {
                    ThumbnailState.Loaded(bytes)
                } else {
                    ThumbnailState.Unavailable
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(
                    "ProfileThumbnail",
                    "Could not load thumbnail for post",
                    e
                )
                ThumbnailState.Unavailable
            }
        } else {
            ThumbnailState.Unavailable
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(AppStyle.colors.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            post.photoCount > 0 &&
                photoState is ThumbnailState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = AppStyle.colors.accent,
                    strokeWidth = 2.dp
                )
            }

            photoState is ThumbnailState.Loaded -> {
                val bytes =
                    (photoState as ThumbnailState.Loaded).bytes

                val request = remember(context, bytes, post.id) {
                    ImageRequest.Builder(context)
                        .data(ByteBuffer.wrap(bytes))
                        .memoryCacheKey("photo:${post.id}:0")
                        .build()
                }

                AsyncImage(
                    model = request,
                    contentDescription = post.caption,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            post.photoCount == 0 && post.imageUrls.isNotEmpty() -> {
                AsyncImage(
                    model = post.imageUrls.first(),
                    contentDescription = post.caption,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            else -> {
                Text(
                    text = if (post.photoCount > 0) {
                        "Photo unavailable"
                    } else {
                        "No image"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppStyle.colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun ProfileStat(
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = AppStyle.colors.textPrimary
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppStyle.colors.textSecondary
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreenContent(
        uiState = ProfileUiState(),
        onEditBio = {},
        onBioDraftChange = {},
        onSaveBio = {},
        onCancelBio = {},
        onPostClick = {}
    )
}
