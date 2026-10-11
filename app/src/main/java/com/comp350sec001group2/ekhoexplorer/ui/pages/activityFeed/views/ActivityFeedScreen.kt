package com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.views

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.views.components.AccountBottomBar
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.views.components.FeedHeader
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.views.components.PostCard
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.models.Vote
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.models.SamplePosts
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.viewmodels.FeedUIState
import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.viewmodels.FeedViewModel
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle


@Composable
fun FeedScreen(
    viewModel: FeedViewModel = viewModel(),
    onAccountClick: () -> Unit = {},
    onSearch: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FeedScreenContent(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onSearch = onSearch,
        onVote = viewModel::onVote,
        onAccountClick = onAccountClick,
    )
}

@Composable
fun FeedScreenContent(
    uiState: FeedUIState,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onVote: (postId: String, vote: Vote) -> Unit,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppStyle.colors.background,
        // The header and bottom bar apply their own system-bar insets.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { FeedHeader(query = uiState.query, onQueryChange = onQueryChange, onSearch = onSearch) },
        bottomBar = { AccountBottomBar(onAccountClick = onAccountClick) },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(items = uiState.posts, key = { it.id }) { post ->
                PostCard(post = post, onVote = { vote -> onVote(post.id, vote) })
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun FeedScreenPreview() = FeedScreenContent(
    uiState = FeedUIState(posts = SamplePosts.all),
    onQueryChange = {},
    onSearch = {},
    onVote = { _, _ -> },
    onAccountClick = {},
)
