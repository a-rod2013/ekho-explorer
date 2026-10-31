package com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.views

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.SamplePosts
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.Vote
import com.comp350sec001group2.ekhoexplorer.ui.components.CommentsSheet
import com.comp350sec001group2.ekhoexplorer.ui.components.FeedHeader
import com.comp350sec001group2.ekhoexplorer.ui.components.PostCard
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.viewmodels.FeedUIState
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.viewmodels.FeedViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.viewmodels.feedViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import com.comp350sec001group2.ekhoexplorer.ui.theme.EkhoExplorerTheme


@Composable
fun FeedScreen(
    viewModel: FeedViewModel = viewModel(factory = feedViewModelFactory),
    onAccountClick: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unreadNotifications by viewModel.unreadNotifications.collectAsStateWithLifecycle()
    var commentsPostId by rememberSaveable { mutableStateOf<String?>(null) }

    FeedScreenContent(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onSearch = onSearch,
        onVote = viewModel::onVote,
        onAccountClick = onAccountClick,
        onRefresh = viewModel::onRefresh,
        onOpenComments = { postId -> commentsPostId = postId },
        onDeletePost = viewModel::onDeletePost,
        onHideUser = viewModel::onHideUser,
        onBlockUser = viewModel::onBlockUser,
        unreadNotifications = unreadNotifications,
        onOpenNotifications = onOpenNotifications,
    )

    commentsPostId?.let { postId ->
        CommentsSheet(postId = postId, onDismiss = { commentsPostId = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreenContent(
    uiState: FeedUIState,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onVote: (postId: String, vote: Vote) -> Unit,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    onOpenComments: (postId: String) -> Unit = {},
    onDeletePost: (postId: String) -> Unit = {},
    onHideUser: (userId: String) -> Unit = {},
    onBlockUser: (userId: String) -> Unit = {},
    unreadNotifications: Int = 0,
    onOpenNotifications: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) snackbarHostState.showSnackbar("Something went wrong. Please try again.")
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppStyle.colors.background,
        // The header and bottom bar apply their own system-bar insets.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },

        // COMMENTING OUT SEARCH BAR FOR SPRINT ONE DEMO PURPOSES, DO NOT DELETE
        // topBar = { FeedHeader(query = uiState.query, onQueryChange = onQueryChange, onSearch = onSearch) },

        // Same header with the search field left out (see above) and the notification bell added.
        topBar = {
            FeedHeader(
                query = uiState.query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                showSearch = false,
                onBellClick = onOpenNotifications,
                unreadNotifications = unreadNotifications,
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = uiState.posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        onVote = { vote -> onVote(post.id, vote) },
                        isOwner = post.userId.isNotEmpty() && post.userId == uiState.currentUserId,
                        onOpenComments = { onOpenComments(post.id) },
                        onDelete = { onDeletePost(post.id) },
                        onHideUser = { onHideUser(post.userId) },
                        onBlockUser = { onBlockUser(post.userId) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun FeedScreenPreview() {
    EkhoExplorerTheme {
        FeedScreenContent(
            uiState = FeedUIState(posts = SamplePosts.all),
            onQueryChange = {},
            onSearch = {},
            onVote = { _, _ -> },
            onAccountClick = {}
        )
    }
}
