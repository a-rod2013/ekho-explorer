package com.comp350sec001group2.ekhoexplorer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

/**
 * The on/off switch for notifications.
 * */
@Composable
fun NotificationsToggleRow(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = AppStyle.dimens
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = d.lg, vertical = d.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "Notifications", style = AppStyle.type.username, color = AppStyle.colors.textPrimary)
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}
