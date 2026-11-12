package com.comp350sec001group2.ekhoexplorer.ui.pages.activity.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp350sec001group2.ekhoexplorer.ui.components.LocalTrailPhotoLoader
import com.comp350sec001group2.ekhoexplorer.ui.pages.activity.viewmodels.TrailListViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.activity.viewmodels.trailListViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    onViewTrails: () -> Unit,
    onOpenTrail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrailListViewModel = viewModel(factory = trailListViewModelFactory),
    sections: List<ActivitySection> = listOf(
        ActivitySection(id = "trails", title = "Trails") {
            PopularTrailsContent(viewModel = viewModel, onViewTrails = onViewTrails, onOpenTrail = onOpenTrail)
        },
    ),
) {
    val d = AppStyle.dimens
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalTrailPhotoLoader provides viewModel.photos) {
        Column(modifier = modifier.fillMaxSize().background(AppStyle.colors.background)) {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Activities",
                        style = AppStyle.type.username,
                        color = AppStyle.colors.textPrimary,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = d.lg, vertical = d.sm).testTag("activity-title"),
                    )
                    sections.forEach { section ->
                        Text(
                            text = section.title,
                            style = AppStyle.type.username,
                            color = AppStyle.colors.textPrimary,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = d.lg, vertical = d.md).testTag("section-${section.id}"),
                        )
                        section.content()
                    }
                }
            }
        }
    }
}
