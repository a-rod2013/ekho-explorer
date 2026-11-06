
package com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels.ProfileUiState
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels.ProfileViewModel
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Post
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Surface
import com.comp350sec001group2.ekhoexplorer.ui.components.PostCard
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxWidth
import com.comp350sec001group2.ekhoexplorer.ui.components.PostCard
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels.profileViewModelFactory

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(
        factory = profileViewModelFactory
    )
) {
    val uiState = viewModel.uiState
    var selectedPost by remember { mutableStateOf<Post?>(null) }

    ProfileScreenContent(
        uiState = uiState,
        onEditBio = viewModel::startEditingBio,
        onBioDraftChange = viewModel::updateBioDraft,
        onSaveBio = viewModel::saveBio,
        onCancelBio = viewModel::cancelEditingBio,
        onPostClick = { post ->
            selectedPost = post
        }
    )

    selectedPost?.let { post ->
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
                    onVote = {},
                    isOwner = false,
                    onOpenComments = {},
                    onDelete = {},
                    onHideUser = {},
                    onBlockUser = {}
                )
            }
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
    onCancelBio: () -> Unit
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
            contentPadding = PaddingValues(bottom = AppStyle.dimens.lg)
        ) {

            // Profile header
            item(span = { GridItemSpan(3) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppStyle.dimens.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = uiState.avatarUrl,
                        contentDescription = "Profile picture",
                        modifier = Modifier.size(80.dp),
                        contentScale = ContentScale.Crop
                    )

                    Column(
                        modifier = Modifier.padding(start = AppStyle.dimens.md),
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

            // Profile statistics
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

                    ProfileStat(
                        count = uiState.species,
                        label = "Species"
                    )
                }
            }

            // Bio
            item(span = { GridItemSpan(3) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppStyle.dimens.lg)
                        .padding(vertical = AppStyle.dimens.xl)
                ) {
                    if (uiState.isEditingBio) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BasicTextField(
                                value = uiState.bioDraft,
                                onValueChange = onBioDraftChange,
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = onCancelBio) {
                                    Text("Cancel")
                                }

                                TextButton(onClick = onSaveBio) {
                                    Text("Save Edits")
                                }
                            }
                        }
                    } else {
                        Text(
                            text = uiState.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppStyle.colors.textPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = AppStyle.dimens.xl)
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

            // Post divider
            item(span = { GridItemSpan(3) }) {
                HorizontalDivider(
                    color = AppStyle.colors.divider
                )
            }

            // Post thumbnails
            items(
                items = uiState.posts,
                key = { post -> post.id }
            ) { post ->

                val thumbnailUrl = post.imageUrls.firstOrNull()

                if (thumbnailUrl != null) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = post.caption,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clickable {
                                onPostClick(post)
                            },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No image",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppStyle.colors.textSecondary
                        )
                    }
                }
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
