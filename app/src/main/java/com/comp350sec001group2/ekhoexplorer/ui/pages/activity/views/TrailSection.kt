package com.comp350sec001group2.ekhoexplorer.ui.pages.activity.views

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailRules
import com.comp350sec001group2.ekhoexplorer.Business.Trails.Viewer
import com.comp350sec001group2.ekhoexplorer.ui.components.EmptyState
import com.comp350sec001group2.ekhoexplorer.ui.components.TrailCard
import com.comp350sec001group2.ekhoexplorer.ui.components.TrailSortBar
import com.comp350sec001group2.ekhoexplorer.ui.pages.activity.viewmodels.TrailListViewModel
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailsSectionContent(
    viewModel: TrailListViewModel,
    onOpenTrail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val d = AppStyle.dimens
    // Only the signed-in user's own id matters for the owner actions on a card.
    val me = Viewer(userId = state.currentUserId, isAdmin = false, following = emptySet())

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = viewModel::onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { TrailSortBar(selected = state.sort, ascending = state.ascending, onSort = viewModel::onSort) }

            state.locationNote?.let { note ->
                item {
                    Text(
                        text = note,
                        style = AppStyle.type.caption,
                        color = AppStyle.colors.textSecondary,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = d.lg, vertical = d.xs).testTag("location-note"),
                    )
                }
            }
            if (state.hasError) {
                item {
                    Text(
                        text = "Something went wrong. Pull down to try again.",
                        style = AppStyle.type.caption,
                        color = AppStyle.colors.downvote,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = d.lg, vertical = d.xs).testTag("trail-error"),
                    )
                }
            }
            if (state.trails.isEmpty() && !state.isLoading && !state.isRefreshing) {
                item {
                    EmptyState(
                        title = "No trails yet",
                        message = "Create one, or check back soon. Pull down to refresh.",
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                    )
                }
            }
            items(items = state.trails, key = { it.id }) { trail ->
                TrailCard(
                    trail = trail,
                    distanceMeters = state.distances[trail.id],
                    isOwner = trail.ownerId == state.currentUserId && state.currentUserId.isNotEmpty(),
                    canRequestVerification = TrailRules.canRequestVerification(trail, me),
                    onClick = { onOpenTrail(trail.id) },
                    onRequestVerification = { viewModel.onRequestVerification(trail.id) },
                )
            }
        }
    }
}
