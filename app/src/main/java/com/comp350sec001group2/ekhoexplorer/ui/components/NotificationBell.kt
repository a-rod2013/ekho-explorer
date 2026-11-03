package com.comp350sec001group2.ekhoexplorer.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

/** Bell with a count badge for unread notifications. [0, 99]. */
@Composable
fun NotificationBell(unreadCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                        Text(if (unreadCount > MAX_SHOWN) "$MAX_SHOWN+" else unreadCount.toString())
                    }
                }
            },
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = if (unreadCount > 0) "Notifications, $unreadCount unread" else "Notifications",
                tint = AppStyle.colors.textPrimary,
            )
        }
    }
}

private const val MAX_SHOWN = 99
