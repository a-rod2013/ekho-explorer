package com.comp350sec001group2.ekhoexplorer.ui.pages.activity.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
import com.comp350sec001group2.ekhoexplorer.ui.pages.activity.viewmodels.TrailListViewModel
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

@Composable
fun PopularTrailsContent(
    viewModel: TrailListViewModel,
    onViewTrails: () -> Unit,
    onOpenTrail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val d = AppStyle.dimens
    val me = Viewer(userId = state.currentUserId, isAdmin = false, following = emptySet())

    Column(modifier = modifier.fillMaxWidth()) {
        if (state.popular.isEmpty() && !state.isLoading && !state.isRefreshing) {
            EmptyState(
                title = "No trails yet",
                message = "Create one, or check back soon. Pull down to refresh.",
                modifier = Modifier.fillMaxWidth().height(200.dp),
            )
        }
        if (state.hasError) {
            Text(
                text = "Something went wrong. Pull down to try again.",
                style = AppStyle.type.caption,
                color = AppStyle.colors.downvote,
                modifier = Modifier.fillMaxWidth().padding(horizontal = d.lg, vertical = d.xs).testTag("trail-error"),
            )
        }
        state.popular.forEach { trail ->
            TrailCard(
                trail = trail,
                distanceMeters = state.distances[trail.id],
                isOwner = trail.ownerId == state.currentUserId && state.currentUserId.isNotEmpty(),
                canRequestVerification = TrailRules.canRequestVerification(trail, me),
                onClick = { onOpenTrail(trail.id) },
                onRequestVerification = { viewModel.onRequestVerification(trail.id) },
            )
        }
        Button(
            onClick = onViewTrails,
            modifier = Modifier.fillMaxWidth().padding(horizontal = d.lg, vertical = d.md).testTag("view-trails"),
        ) { Text("View trails") }
    }
}
