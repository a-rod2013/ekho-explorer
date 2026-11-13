package com.comp350sec001group2.ekhoexplorer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

@Composable
fun HeartButton(
    count: Int,
    hearted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = AppStyle.dimens
    val tint = if (hearted) AppStyle.colors.downvote else AppStyle.colors.textSecondary
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(d.pill))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = d.touchTarget)
            .padding(horizontal = d.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d.xs),
    ) {
        Icon(
            imageVector = if (hearted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (hearted) "Remove heart" else "Heart",
            tint = tint,
            modifier = Modifier.size(d.icon),
        )
        Text(text = count.toString(), style = AppStyle.type.count, color = tint)
    }
}
