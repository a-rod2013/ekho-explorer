
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


@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState = viewModel.uiState

    ProfileScreenContent(uiState = uiState)
}

@Composable
fun ProfileScreenContent(uiState: ProfileUiState) {
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

                        // Explorer since
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
        uiState = ProfileUiState()
    )
}
