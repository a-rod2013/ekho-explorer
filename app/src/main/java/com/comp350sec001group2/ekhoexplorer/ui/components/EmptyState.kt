package com.comp350sec001group2.ekhoexplorer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

/**
 * Centered title and explanation for a screen with nothing to show.
 * Inside a LazyColumn use `item { EmptyState(..., modifier = Modifier.fillParentMaxSize()) }`
 * so the list stays scrollable.
 */
@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    val d = AppStyle.dimens
    Column(
        modifier = modifier.padding(horizontal = d.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(d.sm, Alignment.CenterVertically),
    ) {
        Text(
            text = title,
            style = AppStyle.type.username,
            color = AppStyle.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = AppStyle.type.caption,
            color = AppStyle.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
