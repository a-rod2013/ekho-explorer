package com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.views.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

@Composable
fun PostHeaderRow(avatarUrl: String?, username: String, modifier: Modifier = Modifier) {
    val d = AppStyle.dimens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = d.lg, vertical = d.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d.md),
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(d.avatar)
                .clip(CircleShape)
                .background(AppStyle.colors.surface),
        )
        Text(text = username, style = AppStyle.type.username, color = AppStyle.colors.textPrimary)
    }
}
