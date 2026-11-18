package com.comp350sec001group2.ekhoexplorer.ui.pages.activity.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp350sec001group2.ekhoexplorer.ui.components.LocalTrailPhotoLoader
import com.comp350sec001group2.ekhoexplorer.ui.pages.activity.viewmodels.TrailListViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.activity.viewmodels.trailListViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

@Composable
fun AllTrailsScreen(
    onCreateTrail: () -> Unit,
    onOpenTrail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrailListViewModel = viewModel(factory = trailListViewModelFactory),
) {
    val d = AppStyle.dimens
    CompositionLocalProvider(LocalTrailPhotoLoader provides viewModel.photos) {
        Column(modifier = modifier.fillMaxSize().background(AppStyle.colors.background)) {
            TrailsSectionContent(viewModel = viewModel, onOpenTrail = onOpenTrail)
        }
    }
}
