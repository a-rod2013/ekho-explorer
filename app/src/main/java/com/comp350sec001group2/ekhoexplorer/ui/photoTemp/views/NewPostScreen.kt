package com.comp350sec001group2.ekhoexplorer.ui.photoTemp.views

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.comp350sec001group2.ekhoexplorer.ui.photoTemp.viewmodels.NewPostViewModel
import com.comp350sec001group2.ekhoexplorer.ui.photoTemp.viewmodels.newPostViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

/**
 * TEMPORARY page for making a post while the real create-post flow does not exist. It picks up to
 * five photos with the system photo picker (no permission needed), shrinks them, and posts.
 */
@Composable
fun NewPostScreen(
    onBack: () -> Unit,
    viewModel: NewPostViewModel = viewModel(factory = newPostViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val d = AppStyle.dimens

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(viewModel.photoLimit),
    ) { uris -> viewModel.onPhotosPicked(uris) }

    LaunchedEffect(state.posted) { if (state.posted) onBack() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppStyle.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppStyle.colors.background)
                    .statusBarsPadding()
                    .padding(horizontal = d.sm, vertical = d.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, enabled = !state.isPosting) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = AppStyle.colors.textPrimary)
                }
                Text("New post (temporary)", style = AppStyle.type.username, color = AppStyle.colors.textPrimary)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(d.lg),
            verticalArrangement = Arrangement.spacedBy(d.md),
        ) {
            OutlinedTextField(
                value = state.caption,
                onValueChange = viewModel::onCaptionChange,
                label = { Text("Caption") },
                enabled = !state.isPosting,
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedButton(
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                enabled = !state.isPosting && state.photos.size < viewModel.photoLimit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Choose photos (${state.photos.size} of ${viewModel.photoLimit})")
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
                items(state.photos, key = { it.toString() }) { uri ->
                    Box(modifier = Modifier.size(96.dp).clip(RoundedCornerShape(d.radiusSm))) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Chosen photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        IconButton(
                            onClick = { viewModel.onRemovePhoto(uri) },
                            enabled = !state.isPosting,
                            modifier = Modifier.align(Alignment.TopEnd).size(32.dp),
                        ) {
                            Icon(Icons.Filled.Close, "Remove photo", tint = AppStyle.colors.onAccent)
                        }
                    }
                }
            }

            Button(
                onClick = viewModel::onPost,
                enabled = state.canPost,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isPosting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Post")
                }
            }
        }
    }
}
