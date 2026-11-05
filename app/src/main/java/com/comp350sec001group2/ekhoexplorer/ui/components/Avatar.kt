package com.comp350sec001group2.ekhoexplorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

/** Round user picture. Shows the surface color while loading or when [url] is null. */
@Composable
fun Avatar(url: String?, modifier: Modifier = Modifier, size: Dp = AppStyle.dimens.avatar) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(AppStyle.colors.surface),
    )
}
