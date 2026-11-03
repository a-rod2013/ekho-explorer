package com.comp350sec001group2.ekhoexplorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextOverflow
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.AppNotification
import com.comp350sec001group2.ekhoexplorer.Business.Database.Models.NotificationType
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

@Composable
fun NotificationRow(notification: AppNotification, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val d = AppStyle.dimens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (notification.isRead) AppStyle.colors.background else AppStyle.colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = d.lg, vertical = d.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d.md),
    ) {
        Avatar(url = notification.actorAvatarUrl)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${notification.actorName} ${notification.type.message()}",
                style = AppStyle.type.caption,
                color = AppStyle.colors.textPrimary,
            )
            if (notification.preview.isNotBlank()) {
                Text(
                    text = notification.preview,
                    style = AppStyle.type.caption,
                    color = AppStyle.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = timeAgo(notification.createdAtMillis),
                style = AppStyle.type.captionAction,
                color = AppStyle.colors.textSecondary,
            )
        }
        if (!notification.isRead) {
            Box(
                Modifier
                    .size(d.dot)
                    .clip(CircleShape)
                    .background(AppStyle.colors.accent),
            )
        }
    }
}

private fun NotificationType.message(): String = when (this) {
    NotificationType.POST_LIKE -> "liked your post"
    NotificationType.COMMENT_LIKE -> "liked your comment"
    NotificationType.POST_COMMENT -> "commented on your post"
}


internal fun timeAgo(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    if (epochMillis <= 0L) return "now"
    val minutes = ((nowMillis - epochMillis) / MINUTE_MILLIS).coerceAtLeast(0)
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        minutes < 60 * 24 -> "${minutes / 60}h"
        minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)}d"
        else -> "${minutes / (60 * 24 * 7)}w"
    }
}

private const val MINUTE_MILLIS = 60_000L
