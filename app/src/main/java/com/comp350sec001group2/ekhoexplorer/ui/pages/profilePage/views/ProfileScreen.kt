
package com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.views

import android.graphics.Paint
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels.ProfileUiState
import com.comp350sec001group2.ekhoexplorer.ui.pages.profilePage.viewmodels.ProfileViewModel
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.graphics.Color


@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState = viewModel.uiState

    ProfileScreenContent(
        uiState = uiState,
        onEditBio = viewModel::startEditingBio,
        onBioDraftChange = viewModel::updateBioDraft,
        onSaveBio = viewModel::saveBio,
        onCancelBio = viewModel::cancelEditingBio
    )
}

@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    onEditBio: () -> Unit,
    onBioDraftChange: (String) -> Unit,
    onSaveBio: () -> Unit,
    onCancelBio: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppStyle.colors.background
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                // Profile header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppStyle.dimens.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Profile picture
                    AsyncImage(
                        model = uiState.avatarUrl,
                        contentDescription = "Profile picture",
                        modifier = Modifier.size(80.dp),
                        contentScale = ContentScale.Crop
                    )

                    // Profile information
                    Column(
                        modifier = Modifier.padding(
                            start = AppStyle.dimens.md
                        ),
                        verticalArrangement = Arrangement.Center
                    ) {

                        // Display name
                        Text(
                            text = uiState.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            color = AppStyle.colors.textPrimary
                        )

                        // Username
                        Text(
                            text = "@${uiState.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppStyle.colors.textSecondary
                        )

                        // Join date
                        Text(
                            text = "Explorer since ${uiState.joinDate}",
                            modifier = Modifier.padding(top = AppStyle.dimens.sm),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppStyle.colors.textSecondary
                        )
                    }
                }

                // Profile statistics
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

                // Bio container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppStyle.dimens.lg)
                        .padding(vertical = AppStyle.dimens.xl)
                ) {
                    if (uiState.isEditingBio) {
                        // Editing bio
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

                        // Edit icon
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
                                tint = AppStyle.colors.textSecondary,
                            )
                        }
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
        onCancelBio = {}
    )
}
