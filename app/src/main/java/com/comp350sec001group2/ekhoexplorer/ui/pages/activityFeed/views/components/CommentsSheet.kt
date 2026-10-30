package com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.views.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Comment
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.CommentFields
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.viewmodels.CommentsUIState
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.viewmodels.CommentsViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.viewmodels.commentsViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

/** Bottom sheet listing a post's comments, with votes and a box to add one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(postId: String, onDismiss: () -> Unit) {
    val viewModel: CommentsViewModel = viewModel(
        key = "comments_$postId",
        factory = commentsViewModelFactory(postId),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppStyle.colors.background,
    ) {
        CommentsContent(
            uiState = uiState,
            onVote = viewModel::onVote,
            onSend = viewModel::onSend,
            onLoadMore = viewModel::loadMore,
            modifier = Modifier.fillMaxHeight(SHEET_HEIGHT_FRACTION),
        )
    }
}

private const val SHEET_HEIGHT_FRACTION = 0.7f

@Composable
fun CommentsContent(
    uiState: CommentsUIState,
    onVote: (commentId: String, vote: Vote) -> Unit,
    onSend: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = AppStyle.dimens
    Column(modifier = modifier.fillMaxWidth().imePadding()) {
        Text(
            text = "Comments",
            style = AppStyle.type.username,
            color = AppStyle.colors.textPrimary,
            modifier = Modifier.padding(horizontal = d.lg, vertical = d.sm),
        )
        HorizontalDivider(color = AppStyle.colors.divider)

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (uiState.comments.isEmpty() && !uiState.isLoading) {
                item {
                    Text(
                        text = "No comments yet. Be the first.",
                        style = AppStyle.type.caption,
                        color = AppStyle.colors.textSecondary,
                        modifier = Modifier.padding(d.lg),
                    )
                }
            }
            items(items = uiState.comments, key = { it.id }) { comment ->
                CommentRow(comment = comment, onVote = { vote -> onVote(comment.id, vote) })
            }
            if (uiState.hasMore && !uiState.isLoading) {
                item {
                    TextButton(onClick = onLoadMore, modifier = Modifier.padding(horizontal = d.sm)) {
                        Text("Load more comments")
                    }
                }
            }
            if (uiState.error != null) {
                item {
                    Text(
                        text = "Something went wrong. Please try again.",
                        style = AppStyle.type.caption,
                        color = AppStyle.colors.textSecondary,
                        modifier = Modifier.padding(d.lg),
                    )
                }
            }
        }

        HorizontalDivider(color = AppStyle.colors.divider)
        CommentInput(onSend = onSend)
    }
}

@Composable
private fun CommentRow(comment: Comment, onVote: (Vote) -> Unit, modifier: Modifier = Modifier) {
    val d = AppStyle.dimens
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = d.lg, vertical = d.sm),
        horizontalArrangement = Arrangement.spacedBy(d.md),
    ) {
        AsyncImage(
            model = comment.avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(d.avatar)
                .clip(CircleShape)
                .background(AppStyle.colors.surface),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = comment.username, style = AppStyle.type.username, color = AppStyle.colors.textPrimary)
            Text(text = comment.text, style = AppStyle.type.caption, color = AppStyle.colors.textPrimary)
            VoteBar(
                upCount = comment.upCount,
                downCount = comment.downCount,
                myVote = comment.myVote,
                onVote = onVote,
            )
        }
    }
}

@Composable
private fun CommentInput(onSend: (String) -> Unit, modifier: Modifier = Modifier) {
    val d = AppStyle.dimens
    var text by rememberSaveable { mutableStateOf("") }
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = d.lg, vertical = d.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d.sm),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(CommentFields.MAX_TEXT_LENGTH) },
            placeholder = { Text("Add a comment", style = AppStyle.type.searchHint) },
            textStyle = AppStyle.type.searchText,
            maxLines = 4,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            enabled = text.isNotBlank(),
            onClick = {
                onSend(text)
                text = ""
            },
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Post comment",
                tint = if (text.isNotBlank()) AppStyle.colors.accent else AppStyle.colors.textSecondary,
            )
        }
    }
}
