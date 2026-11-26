package com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.views

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Post
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import com.comp350sec001group2.ekhoexplorer.ui.components.CommentsSheet
import com.comp350sec001group2.ekhoexplorer.ui.components.LocalPhotoLoader
import com.comp350sec001group2.ekhoexplorer.ui.components.PostCard
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels.ProfileUiState
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels.UserProfileViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels.UserProfileViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import java.nio.ByteBuffer
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as EkhoApp

    val viewModel: UserProfileViewModel = viewModel(
        factory = UserProfileViewModelFactory(userId, app.container)
    )

    val uiState = viewModel.uiState
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    var commentsPostId by remember { mutableStateOf<String?>(null) }

    CompositionLocalProvider(LocalPhotoLoader provides viewModel.photos) {
        UserProfileScreenContent(
            uiState = uiState,
            onBack = onBack,
            onPostClick = { post -> selectedPost = post },
            modifier = modifier
        )

        selectedPost?.let { selected ->
            val post = uiState.posts.firstOrNull {
                it.id == selected.id
            } ?: selected

            Dialog(onDismissRequest = { selectedPost = null }) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppStyle.dimens.radiusMd),
                    color = AppStyle.colors.background
                ) {
                    PostCard(
                        post = post,
                        onVote = { vote ->
                            viewModel.onPostVote(post.id, vote)
                        },
                        isOwner = false,
                        onOpenComments = {
                            selectedPost = null
                            commentsPostId = post.id
                        },
                        onDelete = {},
                        onHideUser = {},
                        onBlockUser = {}
                    )
                }
            }
        }

        commentsPostId?.let { postId ->
            CommentsSheet(
                postId = postId,
                onDismiss = { commentsPostId = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreenContent(
    uiState: ProfileUiState,
    onBack: () -> Unit,
    onPostClick: (Post) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppStyle.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.username.isNotBlank()) uiState.username else "Profile",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppStyle.colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppStyle.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppStyle.colors.background
                )
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = AppStyle.dimens.lg)
        ) {
            // Profile header (Read-only, no editing or avatar picker)
            item(span = { GridItemSpan(3) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppStyle.dimens.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
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

                        if (uiState.joinDate.isNotBlank()) {
                            Text(
                                text = "Explorer since ${uiState.joinDate}",
                                modifier = Modifier.padding(top = AppStyle.dimens.sm),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppStyle.colors.textSecondary
                            )
                        }
                    }
                }
            }

            // Observations count statistic
            item(span = { GridItemSpan(3) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppStyle.dimens.lg),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileStat(
                        count = uiState.observations,
                        label = "Observations"
                    )
                }
            }

            // Read-only Bio
            item(span = { GridItemSpan(3) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppStyle.dimens.lg)
                        .padding(vertical = AppStyle.dimens.xl)
                ) {
                    Text(
                        text = when {
                            uiState.isLoading -> "Loading profile..."
                            uiState.errorMessage != null -> uiState.errorMessage
                            uiState.bio.isBlank() -> "No bio provided."
                            else -> uiState.bio
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppStyle.colors.textPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Post divider
            item(span = { GridItemSpan(3) }) {
                HorizontalDivider(color = AppStyle.colors.divider)
            }

            // User's post thumbnails
            items(
                items = uiState.posts,
                key = { post -> post.id }
            ) { post ->
                UserProfilePostThumbnail(
                    post = post,
                    onClick = { onPostClick(post) }
                )
            }

            if (uiState.isLoadingPosts) {
                item(span = { GridItemSpan(3) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppStyle.dimens.lg),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AppStyle.colors.accent
                        )
                    }
                }
            }
        }
    }
}

private sealed interface UserProfileThumbnailState {
    data object Loading : UserProfileThumbnailState
    data class Loaded(val bytes: ByteArray) : UserProfileThumbnailState
    data object Unavailable : UserProfileThumbnailState
}

@Composable
private fun UserProfilePostThumbnail(
    post: Post,
    onClick: () -> Unit
) {
    val photoLoader = LocalPhotoLoader.current
    val context = LocalContext.current

    val photoState by produceState<UserProfileThumbnailState>(
        initialValue = UserProfileThumbnailState.Loading,
        post.id,
        post.photoCount,
        photoLoader
    ) {
        value = UserProfileThumbnailState.Loading

        value = if (post.photoCount > 0) {
            try {
                val bytes = photoLoader.load(post.id, 0)
                if (bytes != null) UserProfileThumbnailState.Loaded(bytes) else UserProfileThumbnailState.Unavailable
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("UserProfileThumbnail", "Could not load thumbnail for post", e)
                UserProfileThumbnailState.Unavailable
            }
        } else {
            UserProfileThumbnailState.Unavailable
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
            post.photoCount > 0 && photoState is UserProfileThumbnailState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = AppStyle.colors.accent,
                    strokeWidth = 2.dp
                )
            }
            photoState is UserProfileThumbnailState.Loaded -> {
                val bytes = (photoState as UserProfileThumbnailState.Loaded).bytes
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
                    text = if (post.photoCount > 0) "Photo unavailable" else "No image",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppStyle.colors.textSecondary
                )
            }
        }
    }
}
