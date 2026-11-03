package com.comp350sec001group2.ekhoexplorer.ui.pages.notifications.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.AppNotification
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.NotificationType
import com.comp350sec001group2.ekhoexplorer.ui.components.CommentsSheet
import com.comp350sec001group2.ekhoexplorer.ui.pages.notifications.viewmodels.NotificationsUIState
import com.comp350sec001group2.ekhoexplorer.ui.pages.notifications.viewmodels.NotificationsViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.notifications.viewmodels.notificationsViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.components.InfoMessage
import com.comp350sec001group2.ekhoexplorer.ui.components.NotificationRow
import com.comp350sec001group2.ekhoexplorer.ui.components.NotificationsToggleRow
import com.comp350sec001group2.ekhoexplorer.ui.components.rememberErrorSnackbarState
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import com.comp350sec001group2.ekhoexplorer.ui.theme.EkhoExplorerTheme

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = viewModel(factory = notificationsViewModelFactory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Tapping a notification marks it read and shows the post's comments.
    var openPostId by rememberSaveable { mutableStateOf<String?>(null) }

    NotificationsScreenContent(
        uiState = uiState,
        onBack = onBack,
        onNotificationClick = { notification ->
            if (!notification.isRead) viewModel.onMarkRead(notification.id)
            openPostId = notification.postId
        },
        onMarkAllRead = viewModel::onMarkAllRead,
        onEnabledChange = viewModel::onSetEnabled,
    )

    openPostId?.let { postId ->
        CommentsSheet(postId = postId, onDismiss = { openPostId = null })
    }
}

@Composable
fun NotificationsScreenContent(
    uiState: NotificationsUIState,
    onBack: () -> Unit,
    onNotificationClick: (AppNotification) -> Unit,
    onMarkAllRead: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = AppStyle.dimens
    val snackbarHostState = rememberErrorSnackbarState(uiState.error)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppStyle.colors.background,
        // The bottom bar is applied by the app scaffold; the top bar below handles the status bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppStyle.colors.background)
                    .statusBarsPadding()
                    .padding(horizontal = d.sm, vertical = d.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AppStyle.colors.textPrimary,
                    )
                }
                Text(
                    text = "Notifications",
                    style = AppStyle.type.username,
                    color = AppStyle.colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onMarkAllRead, enabled = uiState.unreadCount > 0) {
                    Text("Mark all read")
                }
            }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                NotificationsToggleRow(enabled = uiState.notificationsEnabled, onEnabledChange = onEnabledChange)
                HorizontalDivider(color = AppStyle.colors.divider)
            }
            if (!uiState.notificationsEnabled) {
                item { InfoMessage("Notifications are off. Turn them on to see likes and comments.") }
            } else if (uiState.notifications.isEmpty()) {
                item { InfoMessage("Nothing yet. Likes and comments on your posts show up here.") }
            } else {
                items(items = uiState.notifications, key = { it.id }) { notification ->
                    NotificationRow(notification = notification, onClick = { onNotificationClick(notification) })
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun NotificationsScreenPreview() {
    EkhoExplorerTheme {
        NotificationsScreenContent(
            uiState = NotificationsUIState(
                notifications = listOf(
                    AppNotification("1", NotificationType.POST_LIKE, "u1", "maya.hikes", null, "p1", null, "Quiet morning on the trail.", false, 0L),
                    AppNotification("2", NotificationType.POST_COMMENT, "u2", "jordan_k", null, "p1", "c1", "Great shot!", true, 0L),
                ),
            ),
            onBack = {},
            onNotificationClick = {},
            onMarkAllRead = {},
            onEnabledChange = {},
        )
    }
}
