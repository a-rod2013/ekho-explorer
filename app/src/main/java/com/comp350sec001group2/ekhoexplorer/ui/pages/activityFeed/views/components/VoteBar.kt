package com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.views.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role

import com.comp350sec001group2.ekhoexplorer.ui.pages.activityFeed.models.Vote
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

@Composable
fun VoteBar(
    upCount: Int,
    downCount: Int,
    myVote: Vote,
    onVote: (Vote) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(AppStyle.dimens.sm)) {
        VoteButton(
            icon = if (myVote == Vote.UP) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
            description = "Thumbs up",
            count = upCount,
            active = myVote == Vote.UP,
            activeColor = AppStyle.colors.upvote,
            onClick = { onVote(Vote.UP) },
        )
        VoteButton(
            icon = if (myVote == Vote.DOWN) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
            description = "Thumbs down",
            count = downCount,
            active = myVote == Vote.DOWN,
            activeColor = AppStyle.colors.downvote,
            onClick = { onVote(Vote.DOWN) },
        )
    }
}

@Composable
private fun VoteButton(
    icon: ImageVector,
    description: String,
    count: Int,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
) {
    val d = AppStyle.dimens
    val tint = if (active) activeColor else AppStyle.colors.textSecondary
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(d.pill))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = d.touchTarget)
            .padding(horizontal = d.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d.xs),
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = tint, modifier = Modifier.size(d.icon))
        Text(text = count.toString(), style = AppStyle.type.count, color = tint)
    }
}
